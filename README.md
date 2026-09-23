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

## Seller submission and approval

- Sellers can use **Submit artifact** to create a `PENDING_APPROVAL` listing.
- Pending listings remain out of the public live catalogue.
- The administrator can use **Pending approvals** to approve a listing, which changes it to `LIVE`.

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
