package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CompanyApiController {
    private final InMemoryCompanyStore inMemoryCompanyStore;

    public CompanyApiController(InMemoryCompanyStore inMemoryCompanyStore) {
        this.inMemoryCompanyStore = inMemoryCompanyStore;
    }

    @GetMapping("/api/companies")
    public List<SearchSuggestion> searchCompanies(@RequestParam(name = "q", defaultValue = "") String query) {
        return inMemoryCompanyStore.searchByName(query, 12);
    }
}
