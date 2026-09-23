# Redacted production discovery summary

Discovery date: 2026-09-23.

## Repository evidence

- Branch: `main`. The working tree already contained the in-progress Step 1–6 implementation and a separately modified presentation; all were preserved.
- Remote: one GitHub origin for the Artifact Alley repository. GitHub CLI authentication was available, but no push, branch, pull request, workflow run, or deployment was triggered.
- Packaging: Java 21, Spring Boot 3.5.7, Maven executable WAR, JSP/JSTL, Docker.
- Databases before this step: file-backed H2 locally and environment-configured PostgreSQL in production. Hibernate used automatic schema update and no Flyway history existed in repository configuration.
- Current data model: users, artifacts, bids, artifact images, review/winner metadata, and persistent login-throttle records. Security audit events are structured logs rather than database rows.
- Image behavior before this step: validated/re-encoded JPEG and PNG bytes were stored in a local directory through `ArtifactImageStorage`; this is not durable on Render.
- Production variable names already documented: `SPRING_PROFILES_ACTIVE`, `JDBC_DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `INITIAL_ADMIN_PASSWORD`, and Render-supplied `PORT`. No values were inspected or recorded.
- No tracked `.env`, key, credential, dump, or backup filename was found by the repository audit. The repository history was inspected by filenames only; no secret values were printed.

## External state not accessible

No Render control-plane credential, Neon credential, Cloudinary account, live service URL, or production database connection was available in the workspace. Therefore the current Render settings and deployed commit, Neon schema and row counts, active backup/retention features, administrator count, existing production image files, and live behavior were not inspected. No external resource was created or modified.

The implementation consequently uses a conservative split: empty databases migrate through V1 automatically; any nonempty database without Flyway history fails safely until an operator completes the documented backup, schema comparison, isolated restore rehearsal, and explicitly authorized version-1 baseline. Production image migration is similarly disabled until an operator inventories and migrates retained local originals.
