package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class LocalCsvRepository {
    private static final Logger log = LoggerFactory.getLogger(LocalCsvRepository.class);

    private final Path rawDataDirectory;

    public LocalCsvRepository(AppProperties properties) {
        this.rawDataDirectory = properties.dataDirectoryPath().resolve("raw");
    }

    public boolean saveIfChanged(int year, byte[] csvBytes) {
        try {
            Files.createDirectories(rawDataDirectory);
            Path target = rawDataDirectory.resolve(year + ".csv");
            if (!Files.exists(target)) {
                Files.write(target, csvBytes);
                log.info("Stored new CSV for year {} at {} ({} bytes).", year, target, csvBytes.length);
                return true;
            }

            String oldHash = sha256(Files.readAllBytes(target));
            String newHash = sha256(csvBytes);
            if (oldHash.equals(newHash)) {
                log.debug("CSV unchanged for year {} (path={}).", year, target);
                return false;
            }

            Files.write(target, csvBytes);
            log.info("Updated CSV for year {} at {} ({} bytes).", year, target, csvBytes.length);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save CSV for year " + year, e);
        }
    }

    public Map<Integer, Path> listStoredCsvFiles() {
        Map<Integer, Path> files = new HashMap<>();
        if (!Files.exists(rawDataDirectory)) {
            return files;
        }
        try (Stream<Path> stream = Files.list(rawDataDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".csv"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String yearText = fileName.substring(0, fileName.length() - 4);
                        try {
                            files.put(Integer.parseInt(yearText), path);
                        } catch (NumberFormatException ignored) {
                            // Ignore unknown files in data folder.
                        }
                    });
            log.info("Discovered {} local CSV files in {}.", files.size(), rawDataDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to list local CSV files", e);
        }
        return files;
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
