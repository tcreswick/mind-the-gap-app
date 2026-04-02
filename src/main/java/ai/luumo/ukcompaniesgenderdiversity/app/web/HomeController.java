package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyViewCount;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.RecentSubmissionItem;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreMetadata;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryPageViewStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    public static final String HIDDEN_VIEW_STATS_PATH = "/ops/tim-company-views-9f4k2";

    private final InMemoryCompanyStore store;
    private final InMemoryPageViewStore pageViewStore;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final String buildCommitShort;

    public HomeController(InMemoryCompanyStore store,
                          InMemoryPageViewStore pageViewStore,
                          ObjectMapper objectMapper,
                          AppProperties properties,
                          Optional<GitProperties> gitProperties) {
        this.store = store;
        this.pageViewStore = pageViewStore;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.buildCommitShort = gitProperties
                .map(this::extractShortCommitId)
                .orElse(null);
    }

    @GetMapping("/")
    public String home(Model model, HttpServletResponse response, HttpServletRequest request) {
        applyNoStoreHeaders(response);
        applyIndexingHeaders(response, true);
        populateBaseModel(model);
        applySeoModel(model, request, null, false);
        model.addAttribute("viewingCompany", false);
        return "index";
    }

    @GetMapping("/company/{employerId}")
    public String company(@PathVariable String employerId, Model model, HttpServletResponse response, HttpServletRequest request) throws JsonProcessingException {
        applyNoStoreHeaders(response);
        populateBaseModel(model);
        Optional<CompanyHistory> company = store.getByEmployerId(employerId);
        model.addAttribute("viewingCompany", company.isPresent());
        if (company.isPresent()) {
            applyIndexingHeaders(response, true);
            applySeoModel(model, request, company.get(), false);
            CompanyHistory history = company.get();
            List<CompanyYearSummary> summaries = history.yearlySummaries();
            model.addAttribute("companyViewCountLast30Days",
                    pageViewStore.countViewsLast30Days(history.employerId(), Instant.now()));
            model.addAttribute("company", history);
            model.addAttribute("chartPayload", buildChartPayload(summaries));
            model.addAttribute("latestSummary", summaries.get(summaries.size() - 1));
            model.addAttribute("yearlySummariesDesc", summaries.reversed());
            model.addAttribute("companySummary", buildSummary(history.displayName(), summaries));
            model.addAttribute("hasLateSubmissions",
                    summaries.stream().anyMatch(CompanyYearSummary::submittedAfterDeadline));
            model.addAttribute("lateYears",
                    summaries.stream().filter(CompanyYearSummary::submittedAfterDeadline)
                            .map(CompanyYearSummary::reportingYear).collect(Collectors.toSet()));
            model.addAttribute("submittedAtByYear", summaries.stream()
                    .filter(s -> s.submittedAt() != null)
                    .collect(Collectors.toMap(
                            CompanyYearSummary::reportingYear,
                            s -> formatSubmittedAt(s.submittedAt())
                    )));
            summaries.stream()
                    .map(CompanyYearSummary::submittedAt)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .ifPresent(submittedAt -> model.addAttribute("lastSubmissionReceivedFormatted", formatSubmittedAt(submittedAt)));
        } else {
            applyIndexingHeaders(response, false);
            applySeoModel(model, request, null, true);
            model.addAttribute("missingCompanyId", employerId);
        }
        return "index";
    }

    @GetMapping(HIDDEN_VIEW_STATS_PATH)
    public String hiddenViewStats(Model model, HttpServletResponse response) {
        applyNoStoreHeaders(response);
        applyIndexingHeaders(response, false);
        Instant now = Instant.now();
        List<CompanyViewCount> topViews = pageViewStore.topViewedLast30Days(10, now);
        model.addAttribute("generatedAt", UPDATED_FMT.format(now));
        model.addAttribute("companiesWithViewsLast30Days", pageViewStore.companiesWithViewsLast30Days(now));
        model.addAttribute("totalViewsLast30Days", pageViewStore.totalViewsLast30Days(now));
        model.addAttribute("topViewedCompaniesLast30Days", topViews.stream()
                .map(this::toHiddenViewStatItem)
                .toList());
        return "hidden-view-stats";
    }

    private static final DateTimeFormatter UPDATED_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy 'at' HH:mm 'UTC'", Locale.UK)
                    .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter SUBMITTED_FMT =
            DateTimeFormatter.ofPattern("EEEE, d MMM yyyy 'at' HH:mm 'UTC'", Locale.UK)
                    .withZone(ZoneOffset.UTC);

    private void populateBaseModel(Model model) {
        var snapshot = store.snapshot();
        StoreMetadata metadata = snapshot.metadata();
        Instant lastUpdated = metadata.lastUpdatedAt();
        model.addAttribute("lastUpdatedAt", lastUpdated);
        if (lastUpdated != null) {
            model.addAttribute("lastUpdatedFormatted", UPDATED_FMT.format(lastUpdated));
            model.addAttribute("lastUpdatedAgo", formatRelativeTime(lastUpdated));
        }
        List<RecentSubmissionItem> recentSubmissions = snapshot.recentSubmissions() == null
                ? Collections.emptyList()
                : snapshot.recentSubmissions();
        model.addAttribute("recentUpdates", recentSubmissions.stream()
                .map(this::toHomeRecentUpdate)
                .toList());
        model.addAttribute("topViewedCompaniesLast30Days", pageViewStore.topViewedLast30Days(10, Instant.now()).stream()
                .map(this::toTopViewedCompany)
                .flatMap(Optional::stream)
                .toList());
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

    private HomeRecentUpdate toHomeRecentUpdate(RecentSubmissionItem item) {
        Instant submittedAt = item.submittedAt();
        return new HomeRecentUpdate(
                item.employerId(),
                item.displayName(),
                item.reportingYear(),
                formatSubmittedAt(submittedAt),
                formatRelativeTime(submittedAt)
        );
    }

    private Optional<TopViewedCompany> toTopViewedCompany(CompanyViewCount item) {
        return store.getByEmployerId(item.employerId())
                .map(company -> new TopViewedCompany(company.employerId(), company.displayName(), item.viewCount()));
    }

    private HiddenViewStatItem toHiddenViewStatItem(CompanyViewCount item) {
        Optional<CompanyHistory> company = store.getByEmployerId(item.employerId());
        return new HiddenViewStatItem(
                item.employerId(),
                company.map(CompanyHistory::displayName).orElse("(unknown company)"),
                item.viewCount()
        );
    }

    private static String formatSubmittedAt(Instant submittedAt) {
        return SUBMITTED_FMT.format(submittedAt);
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

    private void applyNoStoreHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, s-maxage=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private void applyIndexingHeaders(HttpServletResponse response, boolean allowIndexing) {
        String robotsPolicy = allowIndexing
                ? "index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1"
                : "noindex, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1";
        response.setHeader("X-Robots-Tag", robotsPolicy);
    }

    private void applySeoModel(Model model, HttpServletRequest request, CompanyHistory company, boolean missingCompany) {
        model.addAttribute("canonicalUrl", buildCanonicalUrl(request));
        model.addAttribute("robotsPolicy", missingCompany
                ? "noindex, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1"
                : "index, follow, max-snippet:-1, max-image-preview:large, max-video-preview:-1");
        model.addAttribute("metaDescription", buildMetaDescription(company, missingCompany));
    }

    private String buildCanonicalUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private String buildMetaDescription(CompanyHistory company, boolean missingCompany) {
        if (missingCompany) {
            return "Search UK employers and explore historical gender pay gap trends, pay quartiles, and bonus gaps.";
        }
        if (company == null || company.yearlySummaries().isEmpty()) {
            return "Track how UK employers' gender pay gap data changes over time in one searchable view.";
        }
        int firstYear = company.yearlySummaries().get(0).reportingYear();
        return "View %s's UK gender pay gap history from %d onwards, including pay quartiles and bonus gap trends."
                .formatted(company.displayName(), firstYear);
    }

    private record HomeRecentUpdate(
            String employerId,
            String displayName,
            int reportingYear,
            String submittedAtFormatted,
            String submittedAgo
    ) {
    }

    private record TopViewedCompany(
            String employerId,
            String displayName,
            long viewCountLast30Days
    ) {
    }

    private record HiddenViewStatItem(
            String employerId,
            String displayName,
            long viewCountLast30Days
    ) {
    }
}
