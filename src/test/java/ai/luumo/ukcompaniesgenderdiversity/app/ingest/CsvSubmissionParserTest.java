package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.YearlySubmission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvSubmissionParserTest {
    @TempDir
    Path tempDir;

    private final CsvSubmissionParser parser = new CsvSubmissionParser();

    @Test
    void parseReadsDateSubmittedAsTimestamp() throws IOException {
        Path csv = tempDir.resolve("sample.csv");
        Files.writeString(csv, """
                EmployerName,EmployerId,CurrentName,CompanyNumber,EmployerSize,SubmittedAfterTheDeadline,DateSubmitted
                Example Ltd,EMP-1,Example Ltd,12345678,250 to 499,False,2026/03/27 13:34:41
                """);

        List<YearlySubmission> parsed = parser.parse(csv, 2026);

        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).submittedAt()).isEqualTo(Instant.parse("2026-03-27T13:34:41Z"));
    }
}
