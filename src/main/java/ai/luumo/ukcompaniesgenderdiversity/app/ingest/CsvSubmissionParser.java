package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.YearlySubmission;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CsvSubmissionParser {
    private static final Logger log = LoggerFactory.getLogger(CsvSubmissionParser.class);
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-zA-Z0-9]");

    public List<YearlySubmission> parse(Path csvPath, int year) {
        log.info("Parsing CSV submissions for year {} from {}.", year, csvPath);
        List<YearlySubmission> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .build();

        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser.getRecords()) {
                Map<String, String> normalized = normalize(record.toMap());
                String employerId = pick(normalized, "employerid", "employerId");
                if (isBlank(employerId)) {
                    continue;
                }

                rows.add(new YearlySubmission(
                        year,
                        employerId.trim(),
                        pick(normalized, "employername"),
                        pick(normalized, "currentname"),
                        pick(normalized, "companynumber"),
                        pick(normalized, "employersize"),
                        parseDouble(pick(normalized, "diffmeanhourlypercent")),
                        parseDouble(pick(normalized, "diffmedianhourlypercent")),
                        parseDouble(pick(normalized, "diffmeanbonuspercent")),
                        parseDouble(pick(normalized, "diffmedianbonuspercent")),
                        parseDouble(pick(normalized, "malebonuspercent")),
                        parseDouble(pick(normalized, "femalebonuspercent")),
                        parseDouble(pick(normalized, "malelowerquartile")),
                        parseDouble(pick(normalized, "femalelowerquartile")),
                        parseDouble(pick(normalized, "malelowermiddlequartile")),
                        parseDouble(pick(normalized, "femalelowermiddlequartile")),
                        parseDouble(pick(normalized, "maleuppermiddlequartile")),
                        parseDouble(pick(normalized, "femaleuppermiddlequartile")),
                        parseDouble(pick(normalized, "maletopquartile")),
                        parseDouble(pick(normalized, "femaletopquartile")),
                        parseBoolean(pick(normalized, "submittedafterthedeadline"))
                ));
            }
            log.info("Parsed {} submission rows for year {}.", rows.size(), year);
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse CSV file: " + csvPath, e);
        }
    }

    private Map<String, String> normalize(Map<String, String> input) {
        Map<String, String> normalized = new HashMap<>();
        input.forEach((k, v) -> normalized.put(normalizeKey(k), v));
        return normalized;
    }

    private String normalizeKey(String key) {
        return NON_ALNUM.matcher(key == null ? "" : key.toLowerCase()).replaceAll("");
    }

    private String pick(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(normalizeKey(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value == null ? "" : value.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
