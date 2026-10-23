# Expenses Tracker

Spring Boot API and Android app to log expenses (amount, category, date, note), edit and delete them, filter by month and category, and see monthly totals per category.

## Run it

Requires Java 21. No database install and no Docker: data is stored in an embedded H2 file database under `./data/`.

```
java -jar expenses-tracker-1.0.0.jar
```

The API is at <http://localhost:8080> (health: `/actuator/health`, API base: `/api/v1`). The contract is in `docs/openapi.yaml`.

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `APP_CURRENCY` | `USD` | ISO currency code (3 uppercase letters) shown in summaries |
| `DB_URL` | `jdbc:h2:file:./data/expenses` | JDBC URL (set with `--spring.profiles.active=postgres` for PostgreSQL) |
| `DB_USER` / `DB_PASSWORD` | `sa` / empty | Database credentials |

### Android app

Install the APK from the GitHub release:

```
adb install expenses-tracker-1.0.0.apk
```

or copy it to the phone and open it (allow "install unknown apps"). The app needs to reach the backend:

- Android emulator: `http://10.0.2.2:8080`
- Real phone: `http://<your computer's LAN IP>:8080`, on the same Wi-Fi, with the firewall allowing the port.

## Build

```
cd backend && ./gradlew check bootJar     # JAR in backend/build/libs
cd android && ./gradlew assembleDebug     # APK in android/app/build/outputs/apk/debug
```

Pushing a `v*` tag builds both artefacts on GitHub Actions and attaches them to the release.
