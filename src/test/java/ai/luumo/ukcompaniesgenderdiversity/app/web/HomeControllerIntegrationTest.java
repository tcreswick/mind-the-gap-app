package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
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
                        new CompanyYearSummary(2022, "Example Ltd", 45.0, 55.0),
                        new CompanyYearSummary(2023, "Example Holdings Ltd", 46.0, 54.0)
                )
        );
        store.replace(new CompanyStoreSnapshot(
                new StoreMetadata(Instant.now(), List.of(2022, 2023), 1, 2),
                List.of(company)
        ));
    }

    @Test
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UK Company Gender Pay Gap Viewer")));
    }

    @Test
    void companySearchApiIsPublic() throws Exception {
        mockMvc.perform(get("/api/companies").param("q", "Example"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-1")));
    }

    @Test
    void nonGetRequestIsDenied() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/companies"))
                .andExpect(status().isForbidden());
    }
}
