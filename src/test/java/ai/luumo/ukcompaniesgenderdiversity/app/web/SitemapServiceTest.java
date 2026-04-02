package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreMetadata;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapServiceTest {
    @Test
    void buildSitemapXmlUsesCompanySubmissionLastmodWithStoreFallback() {
        InMemoryCompanyStore store = new InMemoryCompanyStore();
        Instant storeLastUpdatedAt = Instant.parse("2026-04-02T10:00:00Z");

        CompanyHistory withSubmissionTimestamp = new CompanyHistory(
                "EMP-1",
                "Example With Timestamp",
                null,
                null,
                Set.of(),
                List.of(2025),
                List.of(new CompanyYearSummary(
                        2025,
                        "Example With Timestamp",
                        45.0,
                        55.0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        Instant.parse("2026-03-30T14:20:00Z")
                ))
        );

        CompanyHistory withoutSubmissionTimestamp = new CompanyHistory(
                "EMP-2",
                "Example Without Timestamp",
                null,
                null,
                Set.of(),
                List.of(2025),
                List.of(new CompanyYearSummary(2025, "Example Without Timestamp", 40.0, 60.0))
        );

        store.replace(new CompanyStoreSnapshot(
                new StoreMetadata(storeLastUpdatedAt, List.of(2025), 2, 2),
                List.of(withSubmissionTimestamp, withoutSubmissionTimestamp),
                List.of()
        ));

        SitemapService service = new SitemapService(
                store,
                new AppProperties(
                        "https://example.com/download",
                        "https://example.test/",
                        2018,
                        1000,
                        "target/test-data",
                        "store.json.gz",
                        "company-page-views.json.gz",
                        300000,
                        false
                )
        );

        String xml = service.buildSitemapXml();

        assertThat(xml).contains("<loc>https://example.test/</loc>");
        assertThat(xml).contains("<loc>https://example.test/company/EMP-1</loc>");
        assertThat(xml).contains("<loc>https://example.test/company/EMP-2</loc>");
        assertThat(xml).contains("<lastmod>2026-03-30T14:20:00Z</lastmod>");
        assertThat(xml).contains("<lastmod>2026-04-02T10:00:00Z</lastmod>");
    }
}
