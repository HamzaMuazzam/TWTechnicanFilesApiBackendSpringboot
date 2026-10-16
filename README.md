# TW Notification backend

Spring Boot 3.5 / Java 17 service for TrackingWorld: receives alerts from the tracking server ("S3"),
stores them in PostgreSQL, pushes them to the customer Android app over FCM, tracks delivery per device
and serves the admin dashboard. It also still hosts the legacy technician file-upload API on its original URLs.
`SPEC.md` (repository root) is the contract for every path, field and enum.

## Architecture

```
S3 ──▶ /api/v1/ingest/configurations (uid+type → phones)  then  /api/v1/ingest/alerts ──▶ IngestService (tx per alert)
                                               │ alert + notification(PENDING) + delivery(QUEUED) / discarded_recipient
                                               ▼ after commit
                                           DispatchService (@Async) ──FCM data message──▶ Android app
                                               ▲ every 30 s: redispatch QUEUED/FAILED deliveries whose next_attempt_at passed
Android app ──Bearer JWT (USER)──▶ /api/v1/app/**      ack / read / devices / inbox (keyset paging)
Dashboard   ──Bearer JWT (ADMIN|VIEWER)──▶ /api/v1/admin/**   overview, generic query/export, CRUD, SQL console
```

Package `com.trackingworld.tw`:

| module | contents |
|---|---|
| `files` | legacy `FileUploaderController` / `FileUploaderService` / `FileUtils` (unchanged behaviour) |
| `common` | `ResponseWrapper`, `ApiPaths`, `ErrorCodes`, exceptions + `GlobalExceptionHandler`, CORS / Swagger / async / SPA config, `PhoneNormalizer`, `StartupSeeder` |
| `security` | 3 stateless `SecurityFilterChain`s (ingest API key, app JWT, admin JWT), `JwtService`, `RefreshTokenService`, `RateLimitFilter` |
| `firebase` | `FirebaseConfig`, `FirebaseTokenVerifier`, `FcmSender` (`RealFcmSender` / `LoggingFcmSender`) |
| `alerts` | JPA entities + repositories, `DispatchService`, `DeliveryStateService`, status derivation, retention job, settings |
| `ingest` | `POST /api/v1/ingest/alerts` |
| `app` | app auth, devices, notifications, alert types |
| `admin` | admin auth, overview, generic `query/{entity}` + CSV export (JPA Specifications + field whitelist), detail/CRUD endpoints, read-only SQL console |

Schema is managed by Flyway (`src/main/resources/db/migration/V1__init.sql`, `V2__seed.sql`); Hibernate runs with `ddl-auto=validate`.

## Configuration (environment variables)

| variable | default | purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `jdbc:postgresql://localhost:5432/tw_notification` / `postgres` / `postgres` | PostgreSQL 16 |
| `JWT_SECRET` | dev placeholder | HS256 secret, **min 32 chars** (startup fails otherwise) |
| `FIREBASE_ENABLED` | `false` | `true` = real FCM + Firebase phone-auth verification; `false` = pushes are logged and marked SENT with id `DEV` |
| `FIREBASE_CREDENTIALS_PATH` | `./config/firebase-service-account.json` | service-account JSON |
| `AUTH_DEV_LOGIN_ENABLED` | `false` | enables `POST /api/v1/app/auth/dev-login` (never in production) |
| `ADMIN_INITIAL_PASSWORD` | `ChangeMe123!` | password of the seeded `admin` user (WARN logged when default) |
| `INGEST_INITIAL_API_KEY` | generated | API key of the seeded `S3` source (generated key is logged once) |
| `APP_TIMEZONE` | `Asia/Karachi` | retention cron + per-day aggregation |
| `CORS_ALLOWED_ORIGINS` | `*` | comma-separated origin patterns for the dashboard |
| `SERVER_PORT` | `8089` | |

Profiles: `dev` (`application-dev.yml`) turns on dev-login, `firebase.enabled=false`, `show-sql` and DEBUG logs.

## Running locally

```bash
# 1. PostgreSQL
docker run -d --name tw-pg -p 5432:5432 -e POSTGRES_DB=tw_notification -e POSTGRES_PASSWORD=postgres postgres:16-alpine

# 2. backend (dev profile, no Firebase needed)
export JWT_SECRET=local-dev-secret-local-dev-secret-local-dev
./gradlew bootRun --args='--spring.profiles.active=dev'
#    or: ./gradlew bootJar && java -jar build/libs/app.jar --spring.profiles.active=dev
```

Then: swagger `http://localhost:8089/technician/index.html` (groups files / ingest / app / admin),
health `http://localhost:8089/actuator/health`, dashboard `http://localhost:8089/admin` (once `frontend/` is built).
`./gradlew clean build` runs the unit tests; add `-PskipFrontend` to skip the dashboard build.

The dashboard lives in `frontend/` (React + Vite). `./gradlew bootJar` runs `npm ci` + `npm run build` through the
node-gradle plugin (Node 22 downloaded automatically) and copies `frontend/dist` into `static/admin`; the backend
builds fine when the folder is absent.

## Docker (production)

```bash
cp .env.example .env     # or let install.sh generate one with a random JWT_SECRET
./install.sh             # installs docker if needed, builds, starts postgres + app, follows logs
./install.sh logs | stop
```

`docker-compose.yml` runs `postgres:16-alpine` (data in `./data/pg`) and the app on port 8089, with uploads in
`./data/files`, logs in `./data/logs` and `./config` mounted read-only for the Firebase service account.

## Firebase service account

Firebase console → project → Project settings → Service accounts → *Generate new private key*. Save the JSON as
`config/firebase-service-account.json`, set `FIREBASE_ENABLED=true` and restart. The app must use the same Firebase
project for phone auth; the backend verifies the ID token and reads its `phone_number` claim.

## Ingest example

```bash
# 1) who receives what (upsert by S3 ID; AlertType "*" = all types)
curl -X POST http://localhost:8089/api/v1/ingest/configurations -H "Content-Type: application/json" \
  -d '[{"ID":"C-1","PhoneNumber":"03001234567","UID":"111","AlertType":"*","CrtBY":"s3"}]'
# 2) the alert - recipients are looked up from the configurations
curl -X POST http://localhost:8089/api/v1/ingest/alerts -H "Content-Type: application/json" \
  -d '[{"ID":"A-1","UID":"111","AlertType":"OVERSPEED","Message":"Vehicle ABC-123 exceeded 120 km/h","Lat":24.86,"Lon":67.0}]' 
```

Response `202` with one result per item: `ACCEPTED` or `DUPLICATE` (same `sourceAlertId` from the same source),
plus accepted/discarded counts and the discard reasons (`INVALID_PHONE`, `NO_USER`, `USER_DISABLED`).
Unknown `alertType` codes are created automatically as INFO. More requests: `docs/sample-ingest.http`.

## Default credentials

* Dashboard: `admin` / `ADMIN_INITIAL_PASSWORD` (default `ChangeMe123!`) – change it from *Admins* after first login.
* Ingest source `S3`: `INGEST_INITIAL_API_KEY`, or the key printed once in the startup log; rotate from *Ingest sources*.

Login endpoints are rate limited to 20 requests/minute per IP (`429 RATE_LIMITED`).

## URL compatibility

`server.servlet.context-path=/technician` was removed; the legacy URLs are mapped explicitly and stay byte-identical.

| URL | before | now |
|---|---|---|
| `POST /technician/v1/upload-file` | context `/technician` + `/v1/upload-file` | `@RequestMapping("/technician/v1")` |
| `GET /technician/v1/file-public-path?fileName=` | same | same, still returns `/technician/images/files/<name>` |
| `GET /technician/images/files/{name}` | static `/images/**` under the context path | `spring.mvc.static-path-pattern=/technician/images/**` |
| `GET /technician/index.html` | swagger UI | redirects to `/technician/swagger-ui/index.html` |
| `GET /technician/v3/api-docs` | OpenAPI JSON | same (`springdoc.api-docs.path`), groups at `/technician/v3/api-docs/{files,ingest,app,admin}` |
| `/api/v1/ingest/**`, `/api/v1/app/**`, `/api/v1/admin/**`, `/admin/**` | – | new; the reverse proxy must forward these prefixes too |

All JSON responses use `ResponseWrapper { success, message, timestamp, data }`; on errors `message` is an error
code such as `INVALID_TOKEN`, `USER_DISABLED`, `UNKNOWN_FIELD`, `RATE_LIMITED`.
