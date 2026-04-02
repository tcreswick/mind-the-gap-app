# mind-the-gap-app

`mind-the-gap-app` is a Spring Boot web application for exploring UK companies' gender pay gap history over time.
It ingests annual CSV submissions, compiles company-level history, and serves a searchable UI plus API endpoints for viewing trends and year-by-year summaries.

## Tech stack

- Java 21
- Spring Boot (Web, Thymeleaf, Scheduling, Actuator)
- Maven (`mvnw` wrapper included)

## Local development

Run the app with:

```bash
./mvnw spring-boot:run
```

Then open:

- `http://localhost:8080/`

## Production deployment

For server bootstrap, service setup, and ongoing deployment commands, see:

- [`deploy/README.md`](deploy/README.md)

## Google indexing

- The app serves a runtime sitemap at `GET /sitemap.xml`.
- Set `APP_SITE_BASE_URL` in production so sitemap URLs use the public canonical host.
- `robots.txt` includes a sitemap directive for discovery.

### Post-deploy validation checklist

1. Open `https://<your-domain>/sitemap.xml` and confirm it includes:
   - homepage URL
   - company URLs under `/company/{employerId}`
   - `<lastmod>` timestamps
2. In [Google Search Console](https://search.google.com/search-console), submit `https://<your-domain>/sitemap.xml`.
3. Monitor sitemap and indexing status for:
   - fetch/parsing errors
   - rejected URLs
   - indexed count trends over time.