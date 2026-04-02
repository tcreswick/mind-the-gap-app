package ai.luumo.ukcompaniesgenderdiversity.app.store;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyPageViewSnapshot;
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
public class PageViewSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(PageViewSnapshotService.class);

    private final ObjectMapper objectMapper;
    private final Path snapshotPath;

    public PageViewSnapshotService(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.snapshotPath = appProperties.dataDirectoryPath()
                .resolve("page-views")
                .resolve(appProperties.pageViewsFileName());
    }

    public Optional<CompanyPageViewSnapshot> loadSnapshotIfPresent() {
        if (!Files.exists(snapshotPath)) {
            log.info("Page-view snapshot file not found at {}.", snapshotPath);
            return Optional.empty();
        }
        try (InputStream fileIn = Files.newInputStream(snapshotPath, StandardOpenOption.READ);
             GZIPInputStream gzipIn = new GZIPInputStream(fileIn)) {
            CompanyPageViewSnapshot snapshot = objectMapper.readValue(gzipIn, CompanyPageViewSnapshot.class);
            int companies = snapshot.viewsByEmployerId() == null ? 0 : snapshot.viewsByEmployerId().size();
            log.info("Loaded page-view snapshot from disk: path={}, companiesWithViews={}.", snapshotPath, companies);
            return Optional.of(snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read page-view snapshot file " + snapshotPath, e);
        }
    }

    public void writeSnapshot(CompanyPageViewSnapshot snapshot) {
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
            int companies = snapshot.viewsByEmployerId() == null ? 0 : snapshot.viewsByEmployerId().size();
            log.info("Wrote page-view snapshot to disk: path={}, companiesWithViews={}.", snapshotPath, companies);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write page-view snapshot file " + snapshotPath, e);
        }
    }
}
