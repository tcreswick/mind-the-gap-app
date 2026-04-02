package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Component
public class StoreSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(StoreSnapshotService.class);

    private final ObjectMapper objectMapper;
    private final Path snapshotPath;

    public StoreSnapshotService(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.snapshotPath = appProperties.dataDirectoryPath()
                .resolve("store")
                .resolve(appProperties.snapshotFileName());
    }

    public Optional<CompanyStoreSnapshot> loadSnapshotIfPresent() {
        if (!Files.exists(snapshotPath)) {
            log.info("Snapshot file not found at {}.", snapshotPath);
            return Optional.empty();
        }
        try (InputStream fileIn = Files.newInputStream(snapshotPath, StandardOpenOption.READ);
             GZIPInputStream gzipIn = new GZIPInputStream(fileIn)) {
            CompanyStoreSnapshot snapshot = objectMapper.readValue(gzipIn, CompanyStoreSnapshot.class);
            log.info(
                    "Loaded snapshot from disk: path={}, companies={}, submissions={}, years={}.",
                    snapshotPath,
                    snapshot.metadata().companyCount(),
                    snapshot.metadata().submissionCount(),
                    snapshot.metadata().sourceYearsLoaded()
            );
            return Optional.of(snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read snapshot file " + snapshotPath, e);
        }
    }

    public void writeSnapshot(CompanyStoreSnapshot snapshot) {
        try {
            Files.createDirectories(snapshotPath.getParent());
            Path temp = snapshotPath.resolveSibling(snapshotPath.getFileName() + ".tmp");
            try (OutputStream fileOut = Files.newOutputStream(
                    temp,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            ); GZIPOutputStream gzipOut = new GZIPOutputStream(fileOut)) {
                objectMapper.writeValue(gzipOut, snapshot);
            }
            Files.move(temp, snapshotPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info(
                    "Wrote snapshot to disk: path={}, companies={}, submissions={}, years={}.",
                    snapshotPath,
                    snapshot.metadata().companyCount(),
                    snapshot.metadata().submissionCount(),
                    snapshot.metadata().sourceYearsLoaded()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write snapshot file " + snapshotPath, e);
        }
    }
}
