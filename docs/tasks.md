# Expenses Tracker — Tasks

Contract: `docs/openapi.yaml`. Design and assumptions (A1–A14): `docs/architecture.md`.
Each acceptance criterion (AC) must be verifiable by an automated test unless marked *(manual)*.

## Brief → task traceability

| Brief requirement | Tasks |
|---|---|
| Log expenses (amount, category, date, note) | J2, J3, J4, M2, M4 |
| Edit / delete | J4, M4 |
| Filter by month and category | J5, M3 |
| Monthly totals per category | J6, M5 |
| Spring Boot API + Android app, JAR + APK | J1, M1, I2, I3, I4 |

---

## java-developer (`backend/`)

- [ ] **J1. Project skeleton**
  - Gradle Kotlin DSL + wrapper, Java 21 toolchain, Spring Boot 4, based on `.claude/skills/factory/reference/backend-build.gradle.kts`; `rootProject.name = "expenses-tracker"`, version `1.0.0`.
  - `application.yml` from the reference, app name `expenses-tracker`, H2 file `./data/expenses`, `app.currency: ${APP_CURRENCY:USD}`; `postgres` profile; `.env.example` lists `PORT`, `APP_CURRENCY`, `DB_URL`, `DB_USER`, `DB_PASSWORD`.
  - AC: `./gradlew check` passes (Spotless, SpotBugs, JaCoCo ≥ 80%).
  - AC: `./gradlew bootJar` produces `build/libs/expenses-tracker-1.0.0.jar` and no `-plain` jar.
  - AC: `@SpringBootTest` context-load test passes; `GET /actuator/health` returns `200` with `status: UP`.
  - AC: `AppProperties` rejects a currency that is not 3 uppercase letters (unit test: context/binder fails for `APP_CURRENCY=usd1`).

- [ ] **J2. Domain + persistence**
  - `Category` enum with display names (A4), `Expense` entity, `ExpenseRepository`, Flyway `V1__create_expense.sql` (table, `CHECK (amount > 0)`, index on `(expense_date, category)`).
  - AC: `@DataJpaTest` saves and reloads an expense with `amount 12.50`, scale preserved.
  - AC: Flyway migrates an empty H2 DB and Hibernate `validate` passes (context test).
  - AC: repository query for a date range + optional category returns only matching rows, ordered `date desc, id desc`.
  - AC: inserting `amount = 0` directly via JDBC fails on the DB check constraint.

- [ ] **J3. Create and get expense** — `POST /api/v1/expenses`, `GET /api/v1/expenses/{id}`
  - AC: valid POST → `201`, `Location: /api/v1/expenses/{id}`, body matches `Expense` with `createdAt == updatedAt`.
  - AC: POST with `amount` ≤ 0, > 9999999999.99, or with 3 fraction digits → `400` `application/problem+json`, `type /problems/validation-error`, `errors[].field == "amount"`.
  - AC: missing `category`/`date`/`amount` → `400` with each missing field listed.
  - AC: `date` = tomorrow (fixed `Clock`) → `400`, field `date`; `date` = today → `201`.
  - AC: `note` of 501 chars → `400`; note `"  hi  "` stored as `"hi"`; note `"   "` stored as `null`.
  - AC: unknown category `"PETS"` or malformed JSON → `400`, `type /problems/malformed-request`.
  - AC: `Content-Type: text/plain` → `415` problem details.
  - AC: GET existing → `200`; GET unknown → `404`, `type /problems/not-found`; GET `/api/v1/expenses/abc` → `400`.
  - AC: amounts serialized with scale 2 (`45` posted → `45.00` returned).

- [ ] **J4. Update and delete expense** — `PUT /api/v1/expenses/{id}`, `DELETE /api/v1/expenses/{id}`
  - AC: valid PUT → `200`, all fields replaced, `createdAt` unchanged, `updatedAt` later than before (fixed/advanced `Clock`).
  - AC: PUT applies the same validation as POST (one test per rule category is enough, reusing J3 cases).
  - AC: PUT/DELETE on unknown ID → `404` problem details.
  - AC: DELETE existing → `204`, then GET → `404`.

- [ ] **J5. List and filter** — `GET /api/v1/expenses?month=&category=`
  - AC: no params → all expenses, ordered `date desc, id desc`.
  - AC: `month=2026-10` → only expenses dated 2026-10-01..2026-10-31 inclusive (boundary rows on 09-30 and 11-01 excluded).
  - AC: `category=FOOD` → only FOOD across all months; `month` + `category` → intersection.
  - AC: no matches → `200` with `[]`.
  - AC: `month=2026-13`, `month=2026-1`, `category=PETS` → `400`, `type /problems/validation-error`, `errors[].field` names the parameter.

- [ ] **J6. Monthly summary** — `GET /api/v1/summaries/monthly?month=`
  - AC: with FOOD 12.50 + 48.00, HOUSING 150.00, TRANSPORT 35.00 in 2026-10 and other expenses in 2026-09/2026-11 → `total 245.50`, `count 4`, categories ordered HOUSING, FOOD, TRANSPORT with correct totals and counts.
  - AC: equal totals are ordered by category code ascending.
  - AC: empty month → `total 0.00`, `count 0`, `categories []`.
  - AC: `currency` equals the configured `APP_CURRENCY` (test with `app.currency=EUR`).
  - AC: missing or invalid `month` → `400` problem details.
  - AC: aggregation is done in the DB (`GROUP BY`), verified by a repository test, not by loading all rows.

- [ ] **J7. Metadata** — `GET /api/v1/metadata`
  - AC: returns configured currency and all 11 categories in enum order with display names exactly as in the contract example.

- [ ] **J8. Cross-cutting: errors and logging**
  - `ApiExceptionHandler` covering every row of the error table in architecture.md; request-logging filter.
  - AC: an unexpected `RuntimeException` from the service → `500` problem details with a generic `detail` (no exception message or stack trace in the body).
  - AC: log capture test (`OutputCaptureExtension`) shows one INFO line per request with method, path, status and duration, and does not contain the note text.
  - AC: README section (handed to release-admin) lists env vars and the "trusted network only" warning *(manual)*.

## mobile-developer (`android/`)

- [ ] **M1. Project skeleton**
  - Gradle Kotlin DSL + wrapper, Java 17+ source compatibility as AGP requires (app code in Java), `minSdk 26`, applicationId `com.factory.expenses`, versionName `1.0.0`, Material 3, Navigation, Retrofit + Gson, Lifecycle ViewModel/LiveData. Spotless (google-java-format) and JaCoCo wired into `check`.
  - `network_security_config` permits cleartext HTTP (A12).
  - AC: `./gradlew check` passes; `./gradlew assembleDebug` produces an APK.
  - AC: JaCoCo line coverage ≥ 80% on `data.*` and `*ViewModel` classes.

- [ ] **M2. API client and repository**
  - Retrofit interface for all 7 contract operations; DTOs mirroring `openapi.yaml`; Gson adapters for `BigDecimal`, `LocalDate`, `YearMonth`, `Instant`; problem-details parsing into `ApiError(status, title, detail, List<FieldError>)`.
  - Base URL read from `SharedPreferences`, default `http://10.0.2.2:8080/`.
  - AC (MockWebServer): each operation sends the right method, path, query params and JSON body (`amount` serialized as a number with scale 2, `date` as `YYYY-MM-DD`).
  - AC: contract example bodies from `openapi.yaml` deserialize into the DTOs with exact `BigDecimal` values (`12.50` stays `12.50`).
  - AC: a `400` problem response maps to `ApiError` with field errors; `404` maps to a not-found error; connection failure maps to a network error (no exception escapes the repository).

- [ ] **M3. Expenses list with filters**
  - AC (ViewModel unit test): initial state loads current month (fixed `Clock`) with category "All"; prev/next month and category change trigger a reload with the correct `month`/`category` params.
  - AC: loading, content, empty and error states are exposed; retry re-issues the last request.
  - AC: list row shows date, category display name, amount formatted with the `/metadata` currency, and note preview *(manual / Espresso optional)*.

- [ ] **M4. Add / edit / delete expense**
  - AC (ViewModel): Save is blocked client-side for empty amount, amount ≤ 0, more than 2 decimals, missing category, future date, or note > 500 chars, with an error per field.
  - AC: valid add → `POST`; editing an existing expense → `PUT` with all fields; success emits a "navigate back" event and the list reloads.
  - AC: server `400` field errors are mapped onto the matching form fields.
  - AC: Delete asks for confirmation; confirming → `DELETE`; `204` navigates back; `404` shows "already deleted" and navigates back.
  - AC: category dropdown is populated from `GET /api/v1/metadata` *(manual)*; date picker disallows future dates *(manual)*.

- [ ] **M5. Monthly summary screen**
  - AC (ViewModel): loads `GET /api/v1/summaries/monthly` for the selected month; prev/next changes month; exposes total, count and per-category rows in server order with display names.
  - AC: empty month shows an empty state with total `0.00` formatted in the currency.

- [ ] **M6. Settings: API base URL**
  - AC (unit test): URL validator accepts `http://192.168.1.10:8080/` and `https://host/`, rejects `ftp://x`, `not a url` and empty; trailing `/` added if missing.
  - AC: saved URL is persisted and used by the next Retrofit call (repository test with MockWebServer URL).

- [ ] **M7. Resilience and UX polish**
  - AC: rotating the device on any screen keeps state (ViewModel-held) *(manual)*.
  - AC: with the backend stopped, every screen shows an error state with Retry and the app does not crash *(manual)*.

## integration-engineer (`dist/`, `.github/`, contract tests)

- [ ] **I1. Contract tests against `docs/openapi.yaml`**
  - Tests live in `backend/src/test/java/com/factory/expenses/contract/` (coordinate with java-developer; they run in backend `./gradlew check`). Use an OpenAPI 3.1-capable validator (e.g. `swagger-request-validator-mockmvc`) loading `../docs/openapi.yaml`.
  - AC: for every operation and every documented status code (200/201/204/400/404/415), a MockMvc request is made and the response validates against the contract (status, content type, schema).
  - AC: a deliberate contract violation (e.g. a stubbed response missing the required `id` field) makes the test fail, proving the validator is active.

- [ ] **I2. Executable JAR**
  - AC: `backend/./gradlew bootJar` output copied to `dist/expenses-tracker-1.0.0.jar`.
  - AC: smoke script/test: `java -jar dist/expenses-tracker-1.0.0.jar` in an empty temp dir becomes healthy within 15 s, `POST` then `GET /api/v1/expenses` round-trips, and `./data/expenses.mv.db` is created.

- [ ] **I3. APK**
  - AC: `android/./gradlew assembleRelease` (or `assembleDebug` if no signing config) output copied to `dist/expenses-tracker-1.0.0.apk`; no keystore committed (signing via env vars/secrets if used).
  - AC: `aapt dump badging` (or `apkanalyzer`) shows package `com.factory.expenses`, versionName `1.0.0`, minSdk 26.

- [ ] **I4. CI and release workflows**
  - From `.claude/skills/factory/reference/ci.yml` and `release.yml`.
  - AC: CI runs `./gradlew check` in `backend/` and `android/`, builds the JAR and APK, on every push and PR.
  - AC: pushing tag `v1.0.0` builds both artefacts and attaches `expenses-tracker-1.0.0.jar` and `expenses-tracker-1.0.0.apk` to the GitHub release.
  - AC: workflows contain no secrets in plain text (only `${{ secrets.* }}` references).

- [ ] **I5. End-to-end Android ↔ backend check**
  - AC *(manual, emulator)*: with the JAR running on the host, the APK on an emulator can add, edit, delete, filter by month and category, and show the monthly summary; results recorded in `.factory/qa-report.md` input notes.

## qa-engineer (tests, `.factory/qa-report.md`)

- [ ] **Q1. Requirement coverage review**
  - AC: every row of the traceability table above has at least one passing automated test identified by class and method name in the QA report.

- [ ] **Q2. Gates**
  - AC: `./gradlew check` passes in `backend/` and `android/`; backend line coverage ≥ 80%; no high-priority SpotBugs findings; Spotless clean.

- [ ] **Q3. Exploratory API tests on the built JAR**
  - AC: boundary cases pass against `dist/expenses-tracker-1.0.0.jar`: amount `0.01` and `9999999999.99` accepted, `0`, `-1`, `0.001`, `10000000000` rejected; date today accepted, tomorrow rejected; month boundaries (first/last day) included; leap day `2028-02-29` accepted; note 500 chars accepted, 501 rejected; all errors are `application/problem+json`.
  - AC: p95 < 200 ms for list, summary and create with 10,000 seeded expenses (simple JUnit or script timing, results in the report).

- [ ] **Q4. Security and hygiene review**
  - AC: no secrets, `.env`, keystores or `data/` DB files in git; `.env.example` present; no note contents in logs; README contains the unauthenticated / trusted-network warning.
  - AC: `.factory/qa-report.md` written with `STATUS: PASS` or `STATUS: FAIL` and the commit SHA.
