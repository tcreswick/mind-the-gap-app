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