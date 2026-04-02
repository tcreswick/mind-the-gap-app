package ai.luumo.ukcompaniesgenderdiversity.app.ingest;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GovUkDownloadClient {
    private static final Logger log = LoggerFactory.getLogger(GovUkDownloadClient.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private final AppProperties appProperties;
    private final HttpClient httpClient;

    public GovUkDownloadClient(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public int sourceStartYear() {
        return appProperties.sourceStartYear();
    }

    public URI sourceCsvUrlForYear(int year) {
        return normalizedSourceBaseUri().resolve(Integer.toString(year));
    }

    public byte[] downloadCsvIfPresent(URI csvUrl) {
        HttpRequest request = HttpRequest.newBuilder(csvUrl)
                .GET()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/csv,*/*")
                .header("Referer", appProperties.downloadPageUrl())
                .timeout(Duration.ofSeconds(60))
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("CSV download failed: " + csvUrl + " status=" + response.statusCode());
            }
            log.info("Downloaded CSV from {} ({} bytes).", csvUrl, response.body().length);
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("CSV download failed: " + csvUrl, e);
        } catch (IOException e) {
            throw new IllegalStateException("CSV download failed: " + csvUrl, e);
        }
    }

    private URI normalizedSourceBaseUri() {
        String configured = appProperties.downloadPageUrl();
        String withTrailingSlash = configured.endsWith("/") ? configured : configured + "/";
        return URI.create(withTrailingSlash);
    }
}
