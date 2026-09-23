# Artifact Alley production runbook

## Services and configuration

The deployment requires the existing Git repository, one Render Docker web service, one Neon/PostgreSQL database, and one Cloudinary account. Confirm plan limits, backup features and billing in each active account; repository work did not inspect or change them.

| Variable | Required | Purpose | Example format | Profile |
| --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Yes | Activates production safeguards | `prod` | production |
| `JDBC_DATABASE_URL` | Yes | Credential-free PostgreSQL JDBC endpoint | `jdbc:postgresql://host/database?sslmode=require` | production |
| `DB_USERNAME` | Yes | PostgreSQL user | `artifact_app` | production |
| `DB_PASSWORD` | Yes | PostgreSQL password | secret value | production |
| `APP_BASE_URL` | Yes | Canonical public HTTPS origin | `https://service.example` | production |
| `INITIAL_ADMIN_PASSWORD` | Conditional | First admin only when no admin exists | strong random value | production bootstrap |
| `CLOUDINARY_CLOUD_NAME` | Yes | Cloudinary account namespace | `project-cloud` | production |
| `CLOUDINARY_API_KEY` | Yes | Server-side Cloudinary key | provider key | production |
| `CLOUDINARY_API_SECRET` | Yes | Cloudinary signing secret | secret value | production |
| `PORT` | Render supplies | HTTP listener | `10000` | production |
| `ARTIFACT_IMAGES_DIRECTORY` | Optional | Local filesystem root | `./uploads/artifacts` | local/migration |
| `AUCTION_MINIMUM_INCREMENT` | Optional | Bid increment override | `100.00` | all |

Render must use the Docker runtime and health path `/actuator/health/readiness`. Allow enough initial grace time for image download, Flyway and JSP startup. Keep all variables in Render’s secret settings; do not use a combined Cloudinary URL. The application accepts `INITIAL_ADMIN_PASSWORD` being absent after an administrator already exists. For the first deployment it must be at least 12 characters and must not contain common development words. Sign in as `admin@artifact.com`, rotate it immediately through **Change password**, verify the new login, then remove the bootstrap variable on a later controlled restart.

For a new empty database, create a backup/restore point if supported, set the variables, deploy a reviewed commit, and allow Flyway V1 to create the schema. For an existing database, follow the baseline section before starting the new image. Never let a failed service repeatedly retry an unexplained migration.

## Release gate

Deploy only a reviewed commit for which CI passes. The required command is `mvn clean test package`; CI also verifies the executable WAR, JSP resources, migration, committed JSON, and Docker image. Never deploy with a dirty working tree or from an unreviewed local build.

Before each release, create a Neon restore point or logical backup, record the current application commit and Flyway version, and verify the backup can be listed. Deploy one instance first. Flyway runs before Hibernate and the web server becomes ready only after the database migration and validation succeed. Check `/actuator/health/liveness` and `/actuator/health/readiness`, then sign in with a non-admin test account and load a public auction image. Roll back the application image if readiness or the smoke check fails. Do not undo a schema migration by editing `flyway_schema_history`; use a reviewed forward repair migration or restore the database.

## Existing database adoption

`V1__baseline_schema.sql` is the authoritative initial schema. On an empty database Flyway applies it automatically. Flyway deliberately refuses a non-empty database without schema history.

For an existing Neon database:

1. Put the service in maintenance mode and stop all writers.
2. Take and retain a provider restore point plus a `pg_dump --format=custom` backup.
3. Use a read-only account to inventory tables, columns, types, nullability, constraints, indexes, row counts, sequences, and the current administrator count. Compare that inventory with V1. Do not baseline if any difference is unexplained.
4. Restore the backup into an isolated database. Reconcile differences with reviewed SQL, run application smoke checks, and retain the evidence.
5. Only after the restored copy matches V1, explicitly run Flyway baseline at version `1` against the existing production database. This is a one-time operator action; the application keeps `baseline-on-migrate=false` and will never do it automatically.
6. Start the new release. Confirm Flyway reports version 1, Hibernate validation succeeds, row counts and representative users, artifacts, bids, images, login-throttle records, foreign keys, and indexes remain intact.

If any step fails, stop. Restore to a new database, change the service connection back to the last verified database, and keep the failed database for diagnosis.

After explicit approval, the one-time baseline can be run from a trusted operator machine with the same Flyway 11.7.2 release managed by Spring Boot. Keep the password out of the command and replace every placeholder:

```bash
export FLYWAY_PASSWORD='<read-from-an-approved-secret-store>'
docker run --rm --network='<network-that-can-reach-neon>' \
  -e FLYWAY_PASSWORD \
  flyway/flyway:11.7.2 \
  -url='jdbc:postgresql://<host>/<database>?sslmode=require' \
  -user='<migration-user>' \
  -baselineVersion=1 -baselineDescription='Existing schema verified against V1' baseline
unset FLYWAY_PASSWORD
```

Immediately query `flyway_schema_history` with a read-only tool, verify the baseline row/version, and start the application so Flyway validation and Hibernate validation run. Do not execute this command on an empty database; let V1 migrate it normally.

## Backups and recovery

Use Neon point-in-time recovery when the plan supports it and schedule an encrypted daily logical backup to a separate restricted account. Keep at least 7 daily and 4 weekly copies, subject to the project’s data-retention policy. Never commit dumps. A backup is not accepted until a monthly restore drill creates an isolated database, runs Flyway validation and Hibernate validation, verifies row counts and key relationships, and completes public/login/bid read-only smoke checks.

For recovery, stop writers, select the restore point before the incident, restore into a new database, validate it, rotate database credentials if exposure is suspected, change Render environment variables, and deploy the last compatible image. Record recovery-point loss and recovery duration. Cloudinary assets are outside the database: retain provider backups/versioning as available and keep the image metadata export with each database backup.

## Image migration

Production uses Cloudinary. The migration runner is disabled by default. First preserve the local `uploads/artifacts` directory and database backup. Run one controlled instance with that directory available and `--artifactalley.images.migration.enabled=true`; its default `inventory` mode reports total, available/skipped, and missing/failed counts without uploading. Resolve every missing source. Then run with both `--artifactalley.images.migration.enabled=true` and `--artifactalley.images.migration.mode=migrate`. It uploads each legacy file, reads it back, and updates that image row in a separate transaction. Rows already beginning with `cloudinary:` are skipped, so the run is restartable. Local originals are retained.

The process logs only image IDs and counts. A nonzero failure count stops startup. After a successful run, compare total image rows with migrated/skipped counts, sample every content type and several cover/gallery images over HTTPS, then disable the flag and restart normally. Retain local originals through the backup retention period; delete them only after an independent restore check.

## Incident checks

- Database outage: readiness becomes unhealthy; keep the instance out of traffic and check Neon status, connection limits, TLS settings, and credentials.
- Cloudinary outage: aggregate health reports `imageProvider` down, but liveness and database readiness remain healthy so the platform can continue serving non-image requests. Do not restart-loop the service.
- Failed migration: preserve logs and database, do not alter the schema history table, restore or fix forward in a reviewed migration.
- Authentication attack: inspect structured `SECURITY_AUDIT` events and the bounded login-throttle table. Logs contain internal IDs and request IDs, never passwords, tokens, cookies, provider secrets, or image bytes.

Actuator exposes only `health` and `info`. Health details are hidden. Micrometer records JVM, HTTP, datasource, and `artifactalley.security.events` metrics internally; a metrics endpoint is not public. Configure Render alerts for repeated restarts, failed deploys, 5xx rate, readiness failures, database saturation, and memory pressure.

## Logs, secrets and rollback

Filter logs by the safe `requestId` response header and structured `SECURITY_AUDIT` fields. Review Flyway startup lines, datasource readiness, scheduler failures and image-provider HTTP status categories. Never paste a credential-bearing JDBC URL or authentication request body into an incident ticket.

Rotate a secret by creating the replacement at its provider, updating only the existing Render variable, deploying once, validating, and then revoking the old credential. For the database, verify the replacement with a read-only connection before switching. For Cloudinary, verify upload/read/delete with a nonproduction object. If credential exposure is suspected, preserve audit evidence and rotate immediately.

For an application regression, identify the last known-good commit and confirm it supports the current Flyway schema before selecting Render rollback. A bad health path can be changed back to `/actuator/health/readiness` without changing the database. For bad Cloudinary configuration, correct the separated variables; retained local migration originals and provider objects remain the recovery sources. For a failed schema change, stop writers and forward-fix or restore the paired backup. Never run an improvised down migration.

## Production verification checklist

- CI, H2 migrations, PostgreSQL migration checks and Docker build pass; record any explicit skip.
- Current deployed and rollback commit IDs are recorded, and the database backup/restore point is verified.
- Flyway reports the expected version and Hibernate validation succeeds.
- Readiness and liveness return only `{"status":"UP"}`; sensitive Actuator routes return 404.
- Home, CSS, public search/pagination/detail, JSON and XML API routes work; production Swagger remains disabled.
- H2 console is unavailable; errors omit stack traces; security headers and HTTPS Secure cookie are present.
- Registration/login, invalid CSRF, throttling and wrong-role denials behave safely with dedicated test accounts.
- A disposable seller image persists through a controlled restart/redeploy, approval makes the artifact public, and bidder/settlement behavior still works.
- Database records, image metadata, remote image bytes and Flyway history remain after restart.

Do not use genuine active auctions or private user records for these checks. If live access or safe verification accounts are unavailable, record the item as unverified instead of inferring it from deployment status.
