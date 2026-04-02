package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalCsvRepositoryTest {

    @Test
    void saveIfChangedDetectsChangedContent(@TempDir Path tempDir) {
        LocalCsvRepository repository = new LocalCsvRepository(
                new AppProperties(
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

        boolean first = repository.saveIfChanged(2024, "a,b\n1,2".getBytes());
        boolean second = repository.saveIfChanged(2024, "a,b\n1,2".getBytes());
        boolean third = repository.saveIfChanged(2024, "a,b\n1,3".getBytes());

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isTrue();
    }
}
