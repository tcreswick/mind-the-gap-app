package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreMetadata;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Controller
public class HomeController {
    private final InMemoryCompanyStore store;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final String buildCommitShort;

    public HomeController(InMemoryCompanyStore store,
                          ObjectMapper objectMapper,
                          AppProperties properties,
                          Optional<GitProperties> gitProperties) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.buildCommitShort = gitProperties
                .map(this::extractShortCommitId)
                .orElse(null);
    }

    @GetMapping("/")
    public String home(Model model) {
        populateBaseModel(model);
        model.addAttribute("viewingCompany", false);
        return "index";
    }

    @GetMapping("/company/{employerId}")
    public String company(@PathVariable String employerId, Model model) throws JsonProcessingException {
        populateBaseModel(model);
        Optional<CompanyHistory> company = store.getByEmployerId(employerId);
        model.addAttribute("viewingCompany", company.isPresent());
        if (company.isPresent()) {
            CompanyHistory history = company.get();
            List<CompanyYearSummary> summaries = history.yearlySummaries();
            model.addAttribute("company", history);
            model.addAttribute("chartPayload", buildChartPayload(summaries));
            model.addAttribute("latestSummary", summaries.get(summaries.size() - 1));
            model.addAttribute("yearlySummariesDesc", summaries.reversed());
            model.addAttribute("companySummary", buildSummary(history.displayName(), summaries));
            model.addAttribute("hasLateSubmissions",
                    summaries.stream().anyMatch(CompanyYearSummary::submittedAfterDeadline));
            model.addAttribute("lateYears",
                    summaries.stream().filter(CompanyYearSummary::submittedAfterDeadline)
                            .map(CompanyYearSummary::reportingYear).collect(java.util.stream.Collectors.toSet()));
        } else {
            model.addAttribute("missingCompanyId", employerId);
        }
        return "index";
    }

    private static final DateTimeFormatter UPDATED_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' HH:mm 'UTC'", Locale.UK)
                    .withZone(ZoneOffset.UTC);

    private void populateBaseModel(Model model) {
        StoreMetadata metadata = store.snapshot().metadata();
        Instant lastUpdated = metadata.lastUpdatedAt();
        model.addAttribute("lastUpdatedAt", lastUpdated);
        if (lastUpdated != null) {
            model.addAttribute("lastUpdatedFormatted", UPDATED_FMT.format(lastUpdated));
            model.addAttribute("lastUpdatedAgo", formatRelativeTime(lastUpdated));
        }
        model.addAttribute("sourceYears", metadata.sourceYearsLoaded());
        model.addAttribute("sourceUrl", properties.downloadPageUrl());
        model.addAttribute("companyCount", metadata.companyCount());
        model.addAttribute("submissionCount", metadata.submissionCount());
        if (buildCommitShort != null && !buildCommitShort.isBlank()) {
            model.addAttribute("buildCommitShort", buildCommitShort);
        }
    }

    private static String formatRelativeTime(Instant then) {
        long minutes = Duration.between(then, Instant.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }

    private String buildSummary(String displayName, List<CompanyYearSummary> summaries) {
        int firstYear = summaries.get(0).reportingYear();
        long totalCount = summaries.size();
        long lateCount = summaries.stream().filter(CompanyYearSummary::submittedAfterDeadline).count();

        if (totalCount == 1) {
            return lateCount > 0
                    ? displayName + " submitted data for " + firstYear + ", but it was filed after the deadline."
                    : displayName + " submitted data for " + firstYear + ".";
        }
        if (lateCount == 0) {
            return displayName + " has submitted data every year since " + firstYear
                    + ", with all " + totalCount + " submissions on time.";
        }
        if (lateCount == totalCount) {
            return displayName + " has submitted data every year since " + firstYear
                    + ", but all " + totalCount + " submissions were filed after the deadline.";
        }
        return displayName + " has submitted data every year since " + firstYear
                + ". " + lateCount + " of " + totalCount + " submissions were filed after the deadline.";
    }

    private String buildChartPayload(List<CompanyYearSummary> summaries) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("years", summaries.stream().map(CompanyYearSummary::reportingYear).toList());

        // Headline workforce split (averaged quartiles)
        payload.put("female", summaries.stream().map(s -> safePercent(s.femaleOverallPercent())).toList());
        payload.put("male",   summaries.stream().map(s -> safePercent(s.maleOverallPercent())).toList());

        // Hourly pay gap (null preserved — line chart skips missing points)
        payload.put("diffMeanHourly",   summaries.stream().map(s -> nullable(s.diffMeanHourlyPercent())).toList());
        payload.put("diffMedianHourly", summaries.stream().map(s -> nullable(s.diffMedianHourlyPercent())).toList());

        // Bonus pay gap
        payload.put("diffMeanBonus",   summaries.stream().map(s -> nullable(s.diffMeanBonusPercent())).toList());
        payload.put("diffMedianBonus", summaries.stream().map(s -> nullable(s.diffMedianBonusPercent())).toList());

        // Bonus participation rates
        payload.put("maleBonusPct",   summaries.stream().map(s -> safePercent(s.maleBonusPercent())).toList());
        payload.put("femaleBonusPct", summaries.stream().map(s -> safePercent(s.femaleBonusPercent())).toList());

        // Pay quartiles
        payload.put("maleLowerQ",    summaries.stream().map(s -> safePercent(s.maleLowerQuartile())).toList());
        payload.put("femaleLowerQ",  summaries.stream().map(s -> safePercent(s.femaleLowerQuartile())).toList());
        payload.put("maleLowerMQ",   summaries.stream().map(s -> safePercent(s.maleLowerMiddleQuartile())).toList());
        payload.put("femaleLowerMQ", summaries.stream().map(s -> safePercent(s.femaleLowerMiddleQuartile())).toList());
        payload.put("maleUpperMQ",   summaries.stream().map(s -> safePercent(s.maleUpperMiddleQuartile())).toList());
        payload.put("femaleUpperMQ", summaries.stream().map(s -> safePercent(s.femaleUpperMiddleQuartile())).toList());
        payload.put("maleTopQ",      summaries.stream().map(s -> safePercent(s.maleTopQuartile())).toList());
        payload.put("femaleTopQ",    summaries.stream().map(s -> safePercent(s.femaleTopQuartile())).toList());

        return objectMapper.writeValueAsString(payload);
    }

    private double safePercent(Double value) {
        if (value == null || value.isNaN()) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
    }

    private Double nullable(Double value) {
        return (value == null || value.isNaN()) ? null : value;
    }

    private String extractShortCommitId(GitProperties gitProperties) {
        String shortCommit = gitProperties.getShortCommitId();
        if (shortCommit != null && !shortCommit.isBlank()) {
            return shortCommit;
        }
        String fullCommit = gitProperties.getCommitId();
        if (fullCommit == null || fullCommit.isBlank()) {
            return null;
        }
        return fullCommit.substring(0, Math.min(7, fullCommit.length()));
    }
}
