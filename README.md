# Spring Lab Library — RWPSF 3305

Midterm (РК) project for *Web Application Development with Spring Framework*, IITU.
Author: Altai Ali, IT1-2409IS.

One Spring Boot 4.1 application that grew through laboratory works 1–7:

| Lab | Topic | Variant |
|-----|-------|---------|
| 1 | First Spring Boot app, REST controller | 2 — `GET /api/reverse` |
| 2 | IoC container, DI, scopes, life cycle | 2 — `reversed` notifier |
| 3 | YAML, profiles, `@ConfigurationProperties`, Actuator | 2 — `app.pagination` |
| 4 | Spring AOP: logging, timing, audit, self-invocation | 1 — call counter aspect |
| 5 | Spring MVC: REST + page controllers, static resources | 1 — `GET /api/books/search` |
| 6 | Forms, Bean Validation, i18n, `@ControllerAdvice` | 1 — ISBN constraint |
| 7 | REST API: DTOs, ProblemDetail, OpenAPI/Swagger UI | 1 — paging |

## Run

```bash
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

* Site: http://localhost:8080/ and http://localhost:8080/books
* API docs: http://localhost:8080/swagger-ui.html
