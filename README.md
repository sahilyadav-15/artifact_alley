# Artifact Alley

An online auction platform for artifacts and antiques, built as a Java syllabus project.

## Auction discovery

The home page performs database-backed discovery using bookmarkable GET parameters. Both the JSP catalogue and `GET /api/v1/artifacts` call the same `ArtifactSearchService`; no external search engine or in-memory sublisting is used.

| Parameter | Meaning | Values or limit |
| --- | --- | --- |
| `q` | Case-insensitive title, description, era and category search | 100 characters maximum; `%` and `_` are treated literally |
| `category` | Artifact category | One of the `Category` enum values |
| `era` | Case-insensitive era text | 60 characters maximum |
| `minPrice`, `maxPrice` | Inclusive current-price range | Nonnegative decimal values; minimum cannot exceed maximum |
| `endingWithin` | Closing-time window | `24h`, `3d`, `7d` |
| `sort` | Controlled ordering | `ending-soon`, `newest`, `price-asc`, `price-desc`, `most-bids` |
| `page` | Human-facing page number | Starts at 1 |
| `size` | Page size | `12` by default; `12`, `24` or `48` accepted |

Discovery always returns `LIVE` artifacts whose closing time is later than the centralized server clock. Pending, rejected, withdrawn, sold, closed and expired rows are excluded regardless of query parameters. The page query projects bid counts and cover-image IDs with the requested results, preventing per-card database lookups.

Example browser URLs:

```text
http://localhost:8080/?q=coin&category=COIN&endingWithin=7d
http://localhost:8080/?era=18th%20century&minPrice=500&maxPrice=5000&sort=price-asc&page=1&size=12
```

## REST API and backend demonstrations

The versioned API is available under `/api/v1`. It is another interface to the same bidding, review and seller services used by the JSP application; JPA entities are never serialized as API responses.

| Method | Path | Role | Purpose | Success |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/artifacts` | Public | Filtered, sorted and paged public artifacts | 200 |
| GET | `/api/v1/artifacts/{id}` | Public | Public artifact detail | 200 |
| GET | `/api/v1/artifacts/{id}/bids` | Public | Masked bidder history | 200 |
| GET | `/api/v1/session/csrf` | Session | Obtain the token required by unsafe requests | 200 |
| POST | `/api/v1/artifacts/{id}/bids` | BIDDER | Place a transactional bid | 201 |
| GET | `/api/v1/seller/artifacts` | SELLER | List owned artifacts | 200 |
| POST | `/api/v1/seller/artifacts` | SELLER | Submit for review | 201 |
| PUT | `/api/v1/seller/artifacts/{id}` | SELLER | Update an eligible owned listing | 200 |
| POST | `/api/v1/seller/artifacts/{id}/resubmit` | SELLER | Resubmit a rejected listing | 200 |
| POST | `/api/v1/seller/artifacts/{id}/withdraw` | SELLER | Withdraw without deleting history | 204 |
| GET | `/api/v1/admin/artifacts/pending` | ADMIN | Review queue | 200 |
| POST | `/api/v1/admin/artifacts/{id}/approve` | ADMIN | Approve a pending listing | 200 |
| POST | `/api/v1/admin/artifacts/{id}/reject` | ADMIN | Reject with a reason | 200 |
| GET | `/api/v1/admin/reports/auction-summary` | ADMIN | Spring JDBC aggregates | 200 |
| GET | `/api/v1/admin/reports/artifact-activity` | ADMIN | Direct JDBC prepared date-range query | 200 |

Responses support `application/json` and `application/xml`. Select the representation with `Accept`; POST/PUT bodies also require the matching `Content-Type`.

```bash
curl -H 'Accept: application/json' 'http://localhost:8080/api/v1/artifacts?q=coin&endingWithin=7d&sort=most-bids&page=1&size=12'
curl -H 'Accept: application/xml' 'http://localhost:8080/api/v1/artifacts?category=COIN&sort=price-asc'
```

Protected API requests use the existing HTTP session and manual CSRF protection. Retain one cookie jar, fetch a token before the login form, login, fetch the rotated token, and send it in `X-CSRF-Token` on every POST/PUT/PATCH/DELETE. A shell example is:

```bash
csrf=$(curl -s -c cookies.txt http://localhost:8080/api/v1/session/csrf | jq -r .token)
curl -b cookies.txt -c cookies.txt -X POST -d "_csrf=$csrf" -d 'email=your-account@example.com' --data-urlencode 'password=your-password' http://localhost:8080/login
csrf=$(curl -s -b cookies.txt -c cookies.txt http://localhost:8080/api/v1/session/csrf | jq -r .token)
curl -b cookies.txt -H "X-CSRF-Token: $csrf" -H 'Content-Type: application/json' -d '{"amount":1500}' http://localhost:8080/api/v1/artifacts/1/bids
```

Swagger UI is at `http://localhost:8080/swagger-ui.html`; the OpenAPI JSON is at `http://localhost:8080/v3/api-docs`. Open the normal login page in the same browser, then call the CSRF endpoint and supply its value through the documented header scheme. Springdoc v2 is used because its official compatibility line targets Spring Boot 3.

Import [`docs/postman/Artifact-Alley-v1.postman_collection.json`](docs/postman/Artifact-Alley-v1.postman_collection.json) and the optional local environment. Fill credential variables locally, then run the three Session requests in order: obtain CSRF, login, refresh CSRF. Postman retains the cookie and scripts store the token only in the local collection state. The committed files contain no credentials, tokens, cookies, or session IDs.

`AuctionReportJdbcRepository` demonstrates Spring JDBC connection management, parameter binding and row mapping. `ArtifactActivityJdbcRepository` is the deliberately small direct-JDBC example: it obtains a `Connection`, binds a date range on a `PreparedStatement`, reads a `ResultSet`, and closes all three with try-with-resources. Core auction writes continue to use transactional JPA services.

Bid insertion and price update share one transaction, as do settlement and winner assignment. Withdrawal locks the listing before it rechecks bid count. `BidRollbackIntegrationTest` proves a downstream failure rolls back both bid writes; the concurrency and lifecycle suites cover row locking and settlement. The complete topic-by-topic mapping and relationship audit are in [`docs/SYLLABUS_COVERAGE.md`](docs/SYLLABUS_COVERAGE.md).

Run all API, JDBC, transaction, JSP and packaging tests with:

```bash
mvn clean test package
```

## Run locally (H2)

```bash
mvn spring-boot:run
```

Open `http://localhost:8080`. The H2 database console is available at `/h2-console`.

The default `local` Spring profile uses the file-backed H2 database in `data/`. No database environment variables are needed locally.

## Current foundation

- Spring Boot application with constructor dependency injection
- JSP view rendered through Spring MVC
- H2 database with Spring Data JPA entity/repository/service layers
- A catalogue of seeded live auctions
- Registration and sign-in forms with server-side validation
- `BIDDER`, `SELLER`, and `ADMIN` roles; public registration creates bidder or seller accounts only
- HTTP-session access: a password-free `signedInUser` session attribute controls the navbar state, and logout invalidates the session
- Session fixation protection rotates the Servlet session ID and CSRF token after login, registration and role upgrade
- Database-backed login throttling defaults to five failures in fifteen minutes; limits are configurable
- Security headers, escaped JSP output and service-layer database role/ownership checks protect every workflow

Passwords are stored as salted PBKDF2 hashes.

Sessions expire after 30 minutes. Cookies are `HttpOnly` and `SameSite=Lax`; production also requires the `Secure` flag and therefore HTTPS. The full threat model, implemented controls, test evidence and known limitations are in [`docs/SECURITY.md`](docs/SECURITY.md).

## Initial administrator

The application creates this initial administrator only when no administrator account exists. It persists across normal restarts and its password is never overwritten. The password is hashed before storage. Change this initial password immediately after the first sign-in using **Change password**.

- Email: `admin@artifact.com`
- Password: `123456`

`123456` is a local-development default only. A public deployment must provide a strong `INITIAL_ADMIN_PASSWORD` environment variable before its first startup. Never commit a real production password to this repository.

## Deploy on Render with Neon PostgreSQL

This repository is ready for a Docker-based Render web service; deployment and account creation remain controlled operator actions. The complete release, existing-database baseline, image migration, backup, restore, rollback and incident procedures are in [`docs/PRODUCTION_RUNBOOK.md`](docs/PRODUCTION_RUNBOOK.md).

The Docker image runs a packaged Spring Boot executable WAR directly. JSP views stay in the conventional `src/main/webapp/WEB-INF/jsp` layout and are served by embedded Tomcat; no external Tomcat is required.

1. Create a Neon PostgreSQL database and copy its connection details.
2. Create a Render **Web Service** from this repository. Select the Docker runtime; Render builds from the included [Dockerfile](Dockerfile). No separate build or start command is needed.
3. Add these Render environment variables. Set `SPRING_PROFILES_ACTIVE` exactly to `prod`.

| Render variable | Neon value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JDBC_DATABASE_URL` | `jdbc:postgresql://<neon-host>/<database>?sslmode=require` |
| `DB_USERNAME` | Neon database user |
| `DB_PASSWORD` | Neon database password |
| `INITIAL_ADMIN_PASSWORD` | A new strong administrator password |
| `APP_BASE_URL` | Public HTTPS service origin, without a trailing path |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |

Neon usually shows a URL beginning `postgresql://`; for `JDBC_DATABASE_URL`, change that prefix to `jdbc:postgresql://` and keep the required `sslmode=require` query parameter. Put the user and password in `DB_USERNAME` and `DB_PASSWORD`, not in the URL. Render supplies `PORT` automatically, and the application binds to it through `server.port=${PORT:8080}`.

The `prod` profile intentionally has no H2 settings. Flyway is the only schema writer and applies versioned migrations before Hibernate validates the result. Production refuses automatic baselining, non-PostgreSQL URLs, insecure public origins, insecure cookies, missing proxy handling, local image storage, or missing Cloudinary settings. Do not paste secret values into logs, issues, commits, or screenshots.

Configure Render health checks with `/actuator/health/readiness`. Liveness is `/actuator/health/liveness`. Only health and info are exposed, and health details are hidden. Do not point the platform restart check at aggregate `/actuator/health`, because a temporary image-provider outage must be visible without causing a restart loop.

## Seller workspace, review and photos

- Sellers use `/seller/artifacts` to see every listing they own, including pending, rejected, live, sold, closed and withdrawn submissions. Ownership is a persisted `User` relationship; legacy email-only rows are matched case-insensitively when possible and unmatched rows remain visible to administrators for resolution.
- Pending and rejected listings may be edited. Editing a rejected listing does not publish or resubmit it; the seller must explicitly choose **Resubmit for review**. Live, sold, closed and withdrawn listings are read-only.
- The administrator review queue is `/admin/artifacts/pending`. An administrator may approve a valid pending listing or reject it with a required reason of at most 500 characters. Sellers can read that escaped feedback, correct the listing and resubmit it.
- Pending and rejected listings may be withdrawn. A live listing may be withdrawn only while it is open and has no accepted bids; the service locks and rechecks both conditions. Sold, closed and already-withdrawn listings cannot be withdrawn. Withdrawal never deletes bid history or the artifact row.

The listing lifecycle is:

```text
PENDING_APPROVAL → LIVE → SOLD
PENDING_APPROVAL → LIVE → CLOSED
PENDING_APPROVAL → REJECTED → PENDING_APPROVAL
PENDING_APPROVAL → WITHDRAWN
LIVE (open, zero bids) → WITHDRAWN
```

Submissions and eligible drafts accept up to five JPEG or PNG images, each no larger than 5 MB. The server decodes and re-encodes every upload, generates a random storage key, and never uses the supplied filename as a path. Sellers can choose the cover, drag to reorder, and delete photos; deleting the cover chooses the first remaining photo automatically. Seeded and legacy artifacts without images render an accessible placeholder.

Local-development image files default to `./uploads/artifacts`. Override this without changing code:

```properties
artifactalley.images.directory=${ARTIFACT_IMAGES_DIRECTORY:./uploads/artifacts}
```

The production profile uses Cloudinary automatically. Uploads are signed on the server, use random provider identifiers, require HTTPS delivery, and have bounded connection/request timeouts. Credentials stay in environment variables. Existing local files can be moved with the disabled-by-default, restartable procedure in the production runbook; it verifies each provider copy before updating database metadata and keeps local originals for rollback.

### Manual seller and review check

1. Register two seller accounts and one bidder account. Submit a listing with two valid images as Seller A and confirm it appears only in Seller A's **My artifacts** dashboard.
2. Edit the pending listing and use a direct Seller B URL to confirm its details and photo actions are unavailable.
3. Sign in as the administrator, open **Pending approvals**, reject the listing with a reason, then confirm Seller A sees the escaped reason.
4. Edit the rejected listing, reorder/select its cover, resubmit it, approve it as the administrator, and confirm the public catalogue and details page show its cover/gallery.
5. Withdraw a separate live listing with no bids. For another live listing, place a bidder bid first and confirm the seller's withdrawal is rejected while its bid history remains intact.
6. Try an empty file, a file larger than 5 MB, renamed non-image content, and an unsupported type. Confirm each is rejected and no metadata or stored file remains.

## Bidding

Every live catalogue card links to `GET /artifacts/{id}`, which shows the current persisted price, closing time, minimum next bid, and newest-first bid history. Bidder names are masked in public history. Guests are directed to sign in; seller and administrator accounts can view auctions but cannot bid.

Bid submission uses `POST /artifacts/{id}/bids`. The browser sends only the amount. The server obtains the bidder ID from the `signedInUser` session, then reloads both the bidder and artifact from the database. It verifies the bidder role, live status, closing time, listing ownership, and amount before saving anything. A successful POST redirects back to the details page, so refreshing the page does not repeat the bid.

The minimum accepted amount is the current price plus one centralized increment. It defaults to ₹100.00:

```properties
artifactalley.auction.minimum-increment=${AUCTION_MINIMUM_INCREMENT:100.00}
```

The service locks the artifact row with JPA `PESSIMISTIC_WRITE` for the full bid transaction. The bid insert and current-price update commit atomically. If two requests submit against the same old price, the second request rechecks the price after acquiring the lock and is rejected when it no longer meets the new minimum. This strategy works with the local H2 database and PostgreSQL.

### Manual bidding check

1. Start the application with `mvn spring-boot:run` or run the packaged WAR with `java -jar target/artifact-alley-0.0.1-SNAPSHOT.war`.
2. Register a bidder account or sign in with an existing bidder.
3. Open a live catalogue card using **View auction**.
4. Enter the displayed minimum next bid or a larger amount and submit it.
5. Confirm the redirected page shows the new current price and the bid at the top of history.
6. Submit an amount below the newly displayed minimum and confirm the page explains the rejection while the current price remains unchanged.

Automated tests use a separate in-memory H2 database:

```bash
mvn clean test package
```

The suite covers bid rules, atomic failure behavior, controller redirects and validation, history order, and two simultaneous transactions competing for the same previous price.

## Auction lifecycle and settlement

Approved listings follow one of these persisted state paths:

```text
PENDING_APPROVAL → LIVE → SOLD
PENDING_APPROVAL → LIVE → CLOSED
```

- `LIVE` means the closing time is still in the future and the auction may accept valid bidder submissions.
- `SOLD` means the auction ended with at least one valid bid. The highest bid, its bidder, final price, and server settlement time are recorded. It does not mean payment has completed.
- `CLOSED` means the auction ended without a bid. It has no winner and retains its original/current price.

Settlement locks the artifact row with `PESSIMISTIC_WRITE`, selects the highest bid using amount descending, placement time ascending, and ID ascending, then saves the status, winning bid, final price, and settlement time atomically. Repeated calls return the existing outcome without changing its winner or timestamp. The same row lock coordinates settlement with bid placement, so a bid and settlement cannot both cross the authoritative closing boundary.

The application uses one injectable `Clock`. Production uses the application server's default timezone so existing `LocalDateTime` database values retain their established meaning. All application instances should therefore use the same configured system timezone. Tests replace this clock with a fixed or mutable clock and do not wait for wall-clock time.

A scheduler checks a bounded batch of expired `LIVE` auctions once per minute by default. Each artifact is settled in its own transaction, and one failure does not stop the rest of the batch:

```properties
artifactalley.auction.settlement-interval-ms=${AUCTION_SETTLEMENT_INTERVAL_MS:60000}
artifactalley.auction.settlement-initial-delay-ms=${AUCTION_SETTLEMENT_INITIAL_DELAY_MS:60000}
artifactalley.auction.settlement-batch-size=${AUCTION_SETTLEMENT_BATCH_SIZE:50}
```

Artifact details also request settlement before rendering, while the public catalogue queries only `LIVE` artifacts whose closing time is still in the future. Bidder accounts can open `/account/bids` through **My bids** to see active, won, and lost or ended auctions.

### Manual lifecycle check

1. Start the packaged WAR and register two bidder accounts in separate browser sessions.
2. Open the same active artifact and place two increasing valid bids.
3. In a disposable test profile, advance the injected clock beyond the artifact's closing time or wait for a naturally closing test auction.
4. Open the artifact details page or run the settlement batch.
5. Confirm the artifact is `SOLD`, the highest bidder sees **You won this auction**, the other bidder sees an ended outcome, and the final price matches the winning bid.
6. Attempt another bid and confirm it is rejected.
7. Run settlement again and confirm the winner, final price, and settlement timestamp remain unchanged.

For a no-bid lifecycle check, close an expired `LIVE` test artifact without bids and confirm it becomes `CLOSED` with no winner.
