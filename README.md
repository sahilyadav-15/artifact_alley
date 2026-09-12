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

## Next feature

Add artifact photos and seller-facing management of submitted listings.
