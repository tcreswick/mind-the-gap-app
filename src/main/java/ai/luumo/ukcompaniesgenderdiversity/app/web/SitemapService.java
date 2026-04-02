package ai.luumo.ukcompaniesgenderdiversity.app.web;

import ai.luumo.ukcompaniesgenderdiversity.app.config.AppProperties;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyHistory;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyStoreSnapshot;
import ai.luumo.ukcompaniesgenderdiversity.app.domain.CompanyYearSummary;
import ai.luumo.ukcompaniesgenderdiversity.app.store.InMemoryCompanyStore;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class SitemapService {
    private static final String SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9";

    private final InMemoryCompanyStore store;
    private final String siteBaseUrl;

    public SitemapService(InMemoryCompanyStore store, AppProperties properties) {
        this.store = store;
        this.siteBaseUrl = normalizeSiteBaseUrl(properties.siteBaseUrl());
    }

    public String buildSitemapXml() {
        CompanyStoreSnapshot snapshot = store.snapshot();
        Instant storeLastUpdatedAt = snapshot.metadata().lastUpdatedAt();
        List<SitemapEntry> entries = new ArrayList<>();
        entries.add(new SitemapEntry(buildRootUrl(), storeLastUpdatedAt));

        snapshot.companies().stream()
                .sorted(Comparator.comparing(CompanyHistory::employerId))
                .map(company -> new SitemapEntry(
                        buildCompanyUrl(company.employerId()),
                        latestSubmittedAt(company).orElse(storeLastUpdatedAt)
                ))
                .forEach(entries::add);

        return writeXml(entries);
    }

    private String buildRootUrl() {
        return UriComponentsBuilder.fromUriString(siteBaseUrl)
                .replacePath("/")
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUriString();
    }

    private String buildCompanyUrl(String employerId) {
        return UriComponentsBuilder.fromUriString(siteBaseUrl)
                .pathSegment("company", employerId)
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUriString();
    }

    private String writeXml(List<SitemapEntry> entries) {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);

            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("urlset");
            xml.writeDefaultNamespace(SITEMAP_NS);

            for (SitemapEntry entry : entries) {
                xml.writeStartElement("url");
                xml.writeStartElement("loc");
                xml.writeCharacters(entry.url());
                xml.writeEndElement();

                if (entry.lastModified() != null) {
                    xml.writeStartElement("lastmod");
                    xml.writeCharacters(DateTimeFormatter.ISO_INSTANT.format(entry.lastModified()));
                    xml.writeEndElement();
                }

                xml.writeEndElement();
            }

            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
            return output.toString();
        } catch (XMLStreamException ex) {
            throw new IllegalStateException("Failed to generate sitemap XML", ex);
        }
    }

    private static String normalizeSiteBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static java.util.Optional<Instant> latestSubmittedAt(CompanyHistory company) {
        return company.yearlySummaries().stream()
                .map(CompanyYearSummary::submittedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
    }

    private record SitemapEntry(String url, Instant lastModified) {
    }
}
