package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.RecentSubmissionItem;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.StoreMetadata;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.startup-refresh-enabled=false",
        "app.download-page-url=https://example.com",
        "app.data-directory=target/test-data"
})
@AutoConfigureMockMvc
class HomeControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryCompanyStore store;

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
    }

    @Test
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mind the Gap")))
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
    void companyPageShowsSubmissionTimestamps() throws Exception {
        mockMvc.perform(get("/company/EMP-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Last submission received on")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Submitted on")));
    }

    @Test
    void nonGetRequestIsDenied() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/companies"))
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
