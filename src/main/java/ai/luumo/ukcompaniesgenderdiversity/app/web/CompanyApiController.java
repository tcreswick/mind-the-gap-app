package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryPageViewStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class CompanyApiController {
    private final InMemoryCompanyStore inMemoryCompanyStore;
    private final InMemoryPageViewStore inMemoryPageViewStore;

    public CompanyApiController(InMemoryCompanyStore inMemoryCompanyStore, InMemoryPageViewStore inMemoryPageViewStore) {
        this.inMemoryCompanyStore = inMemoryCompanyStore;
        this.inMemoryPageViewStore = inMemoryPageViewStore;
    }

    @GetMapping("/api/companies")
    public List<SearchSuggestion> searchCompanies(@RequestParam(name = "q", defaultValue = "") String query) {
        return inMemoryCompanyStore.searchByName(query, 12);
    }

    @PostMapping("/api/track/company-view")
    public ResponseEntity<Void> trackCompanyView(@RequestBody CompanyViewTrackRequest request) {
        String employerId = request == null ? null : request.employerId();
        if (employerId != null && !employerId.isBlank() && inMemoryCompanyStore.getByEmployerId(employerId).isPresent()) {
            inMemoryPageViewStore.recordView(employerId, Instant.now());
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .build();
    }

    private record CompanyViewTrackRequest(String employerId) {
    }
}
