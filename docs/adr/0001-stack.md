# ADR 0001: Stack for Expenses Tracker

- **Status:** Proposed (awaiting plan approval)
- **Date:** 2026-10-23

## Context

The brief asks for a Spring Boot API and an Android app to log, edit, delete and filter expenses and to show monthly totals per category. The factory standards require Java 21, Gradle Kotlin DSL, an executable JAR that starts with no setup, an installable APK, and no Docker. The product has one user and a small data volume.

## Decision

- **Backend:** one Spring Boot 4 application (Java 21) with `spring-boot-starter-webmvc`, `-validation`, `-data-jpa`, `-flyway`, `-actuator`. Layered packages `api / domain / service / repository / config` under `com.factory.expenses`.
- **Persistence:** embedded H2 file database (`./data/expenses`, `MODE=PostgreSQL`) with Flyway migrations and `ddl-auto=validate`. PostgreSQL available through the optional `postgres` profile. Tests use H2 in-memory.
- **Auth:** none. Spring Security is not included (single user on a trusted network; see architecture A1).
- **Money:** `BigDecimal` / `NUMERIC(12,2)`, one configurable currency (`APP_CURRENCY`).
- **Categories:** fixed Java enum exposed through `GET /api/v1/metadata`.
- **Errors:** RFC 9457 problem details through Spring `ProblemDetail`.
- **Android:** native Java, MVVM (ViewModel + LiveData), Retrofit + Gson, Navigation, Material 3, `minSdk 26`.
- **Quality:** JUnit 5, AssertJ, Mockito, Spring test slices; JaCoCo ≥ 80% line coverage, Spotless (google-java-format), SpotBugs, all in `./gradlew check`.
- **Delivery:** `expenses-tracker-1.0.0.jar` and `expenses-tracker-1.0.0.apk` attached to a GitHub release by a `v*` tag workflow. No Docker.

## Alternatives considered

- **Category as a table with CRUD:** more flexible, but adds endpoints, screens and referential rules the brief does not ask for. Rejected for v1; the enum can be migrated to a table later without changing the `category` string on the wire.
- **JWT auth with users:** not required by the brief and adds a login flow to the app. Rejected; revisit if multi-user is requested.
- **Amount as integer minor units (cents):** avoids decimals in JSON but couples clients to currency exponents. Rejected in favour of `BigDecimal` with scale 2.
- **Offline-first Android with Room and sync:** large extra scope (conflicts, sync). Rejected; app is online-only.
- **PostgreSQL by default:** needs an install, which breaks "runs with `java -jar` and nothing else". Kept as an optional profile.

## Consequences

- Zero-setup start; data lives in `./data/` next to where the JAR is run.
- The API must not be exposed on untrusted networks; the README must say so.
- Adding users, custom categories or multiple currencies later means a new migration and a `/api/v2` or additive contract change.
