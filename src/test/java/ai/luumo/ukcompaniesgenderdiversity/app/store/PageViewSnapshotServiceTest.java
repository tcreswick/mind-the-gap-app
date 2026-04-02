package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyPageViewSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageViewSnapshotServiceTest {
    @Test
    void writeAndLoadSnapshotRoundTrips(@TempDir Path tempDir) {
        PageViewSnapshotService service = new PageViewSnapshotService(
                new ObjectMapper().findAndRegisterModules(),
                new AppProperties(
                        "https://example.com",
                        "https://example.com",
                        2018,
                        1000,
                        tempDir.toString(),
                        "store.json.gz",
                        "company-page-views.json.gz",
                        300000,
                        false
                )
        );
        Instant now = Instant.parse("2026-04-02T12:00:00Z");
        CompanyPageViewSnapshot expected = new CompanyPageViewSnapshot(Map.of(
                "EMP-1", List.of(now.minusSeconds(60), now),
                "EMP-2", List.of(now.minusSeconds(30))
        ));

        service.writeSnapshot(expected);
        CompanyPageViewSnapshot loaded = service.loadSnapshotIfPresent().orElseThrow();

        assertThat(loaded.viewsByEmployerId()).isEqualTo(expected.viewsByEmployerId());
    }
}
