package ai.luumo.ukcompaniesgenderdiversity.app.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String downloadPageUrl,
        @Min(2017) int sourceStartYear,
        @Min(1) long refreshIntervalMs,
        @NotBlank String dataDirectory,
        @NotBlank String snapshotFileName,
        @NotBlank String pageViewsFileName,
        @Min(1000) long pageViewsPersistIntervalMs,
        boolean startupRefreshEnabled
) {
    public Path dataDirectoryPath() {
        return Path.of(dataDirectory);
    }
}
