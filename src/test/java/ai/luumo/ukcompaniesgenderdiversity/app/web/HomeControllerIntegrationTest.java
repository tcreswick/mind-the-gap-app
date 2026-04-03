package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyPageViewSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.RecentSubmissionItem;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreMetadata;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryPageViewStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.startup-refresh-enabled=false",
        "app.download-page-url=https://example.com",
        "app.site-base-url=https://example.test",
        "app.data-directory=target/test-data"
})
@AutoConfigureMockMvc
class HomeControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryCompanyStore store;

    @Autowired
    private InMemoryPageViewStore pageViewStore;

    @BeforeEach
    void setUpStore() {
        CompanyHistory company = new CompanyHistory(
                "EMP-1",
                "Example Holdings Ltd",
                "12345678",
                "500 to 999",
                Set.of("Example Ltd", "Example Holdings Ltd"),
                List.of(2022, 2023),
                List.of(
                        summaryWithSubmittedAt(2022, "Example Ltd", 45.0, 55.0, "2024-03-28T09:45:00Z"),
                        summaryWithSubmittedAt(2023, "Example Holdings Ltd", 46.0, 54.0, "2025-03-30T14:20:00Z")
                )
        );
        store.replace(new CompanyStoreSnapshot(
                new StoreMetadata(Instant.now(), List.of(2022, 2023), 1, 2),
                List.of(company),
                List.of(new RecentSubmissionItem(
                        "EMP-1",
                        "Example Holdings Ltd",
                        2023,
                        Instant.parse("2026-04-01T12:30:00Z")
                ))
        ));
        pageViewStore.replace(CompanyPageViewSnapshot.empty(), Instant.now());
    }

    @Test
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mind the Gap")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submissions over the last 30 days")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Most recent updates")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/company/EMP-1")));
    }

    @Test
    void companySearchApiIsPublic() throws Exception {
        mockMvc.perform(get("/api/companies").param("q", "Example"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-1")));
    }

    @Test
    void homePageShowsTopViewedBeforeRecentUpdates() throws Exception {
        pageViewStore.recordView("EMP-1", Instant.now());
        pageViewStore.recordView("EMP-1", Instant.now());

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Most viewed in the last 30 days")))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html.indexOf("Most viewed in the last 30 days"))
                .isLessThan(html.indexOf("Most recent updates"));
    }

    @Test
    void homePageShowsSubmissionTrendAboveRecentUpdates() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"home-submissions-trend-data\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"home-submissions-chart\"")))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html.indexOf("Submissions over the last 30 days"))
                .isLessThan(html.indexOf("Most recent updates"));
    }

    @Test
    void hiddenViewStatsPageIsPublicAndShowsSummary() throws Exception {
        pageViewStore.recordView("EMP-1", Instant.now());
        pageViewStore.recordView("EMP-1", Instant.now());

        mockMvc.perform(get(HomeController.HIDDEN_VIEW_STATS_PATH))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", org.hamcrest.Matchers.startsWith("noindex, follow")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Internal view stats (last 30 days)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Example Holdings Ltd")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-1")));
    }

    @Test
    void companyPageShowsSubmissionTimestamps() throws Exception {
        mockMvc.perform(get("/company/EMP-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Last submission received on")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submitted on")));
    }

    @Test
    void pagesExposeIndexingMetadataAndHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", org.hamcrest.Matchers.startsWith("index, follow")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<meta name=\"robots\" content=\"index, follow")));

        mockMvc.perform(get("/company/UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", org.hamcrest.Matchers.startsWith("noindex, follow")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<meta name=\"robots\" content=\"noindex, follow")));
    }

    @Test
    void robotsAndLlmsFilesAreServed() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User-agent: *")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Disallow: /api/")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap: /sitemap.xml")));

        mockMvc.perform(get("/llms.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("# Mind the Gap")));
    }

    @Test
    void sitemapIsPublicAndListsCompanyUrls() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.test/</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<loc>https://example.test/company/EMP-1</loc>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<lastmod>2025-03-30T14:20:00Z</lastmod>")));
    }

    @Test
    void companyPageShowsViewCountSummary() throws Exception {
        pageViewStore.recordView("EMP-1", Instant.now());
        pageViewStore.recordView("EMP-1", Instant.now());

        mockMvc.perform(get("/company/EMP-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"company-view-tracker\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "This company has been viewed 2 times in the last 30 days."
                )));
    }

    @Test
    void homePageDoesNotRenderTrackingElement() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"company-view-tracker\""))));
    }

    @Test
    void trackingEndpointRecordsViewAndSetsNoCacheHeaders() throws Exception {
        mockMvc.perform(post("/api/track/company-view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employerId\":\"EMP-1\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));

        assertThat(pageViewStore.countViewsLast30Days("EMP-1", Instant.now())).isEqualTo(1);
    }

    @Test
    void trackingEndpointIgnoresUnknownEmployer() throws Exception {
        mockMvc.perform(post("/api/track/company-view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employerId\":\"UNKNOWN\"}"))
                .andExpect(status().isNoContent());

        assertThat(pageViewStore.countViewsLast30Days("UNKNOWN", Instant.now())).isEqualTo(0);
    }

    @Test
    void nonGetRequestIsDenied() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/companies"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trackingEndpointGetRequestIsDenied() throws Exception {
        mockMvc.perform(get("/api/track/company-view"))
                .andExpect(status().isForbidden());
    }

    private CompanyYearSummary summaryWithSubmittedAt(
            int reportingYear,
            String employerName,
            Double femaleOverallPercent,
            Double maleOverallPercent,
            String submittedAt
    ) {
        return new CompanyYearSummary(
                reportingYear,
                employerName,
                femaleOverallPercent,
                maleOverallPercent,
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
                Instant.parse(submittedAt)
        );
    }
}
