# Artifact Alley

An online auction platform for artifacts and antiques, built as a Java syllabus project.

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

Passwords are stored as salted PBKDF2 hashes.

## Initial administrator

The application creates this initial administrator only when it is absent. It persists across normal restarts and its password is never overwritten. The password is hashed before storage. Change this initial password immediately after the first sign-in using **Change password**.

- Email: `admin@artifact.com`
- Password: `123456`

`123456` is a local-development default only. A public deployment must provide a strong `INITIAL_ADMIN_PASSWORD` environment variable before its first startup. Never commit a real production password to this repository.

## Deploy on Render with Neon PostgreSQL

This repository is ready for a Docker-based Render web service; deployment and account creation remain manual.

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

Neon usually shows a URL beginning `postgresql://`; for `JDBC_DATABASE_URL`, change that prefix to `jdbc:postgresql://` and keep the required `sslmode=require` query parameter. Put the user and password in `DB_USERNAME` and `DB_PASSWORD`, not in the URL. Render supplies `PORT` automatically, and the application binds to it through `server.port=${PORT:8080}`.

The `prod` profile intentionally has no H2 settings and reads its PostgreSQL URL, user, and password only from the environment. JPA remains `ddl-auto=update` for this demo; use managed migrations before relying on it for a production application.

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

Local image files default to `./uploads/artifacts`. Override this without changing code:

```properties
artifactalley.images.directory=${ARTIFACT_IMAGES_DIRECTORY:./uploads/artifacts}
```

This filesystem implementation is intended for local development. A Render service filesystem may be ephemeral, so images can disappear after a restart or redeploy. Before production use, implement the existing `ArtifactImageStorage` interface with persistent object storage and configure that implementation; this step intentionally does not add a cloud provider.

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
