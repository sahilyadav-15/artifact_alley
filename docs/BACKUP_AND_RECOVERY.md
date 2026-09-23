# Backup and recovery

This procedure covers PostgreSQL/Neon data and Cloudinary images. The active Neon plan and its point-in-time restore window were not accessible during repository work, so confirm current provider capabilities and retention in the Neon console before relying on them.

## Database backups

Use both provider restore points, when the active plan supplies them, and encrypted logical backups stored outside Render and Neon. A suggested starting policy is a daily logical backup with 7 daily and 4 weekly copies; adjust it to the project’s legal and recovery requirements. Restrict backup access to production operators and record successful backup size, time, source database identifier, application commit, and Flyway version without recording credentials.

Keep credentials out of command arguments and shell history. Use a restricted temporary PostgreSQL password file or an interactive secure prompt:

```bash
export PGHOST='<database-host>' PGPORT='5432' PGDATABASE='<database>' PGUSER='<backup-user>'
export PGPASSFILE='<path-to-mode-0600-temporary-pgpass>'
pg_dump --format=custom --no-owner --no-acl --file='<encrypted-backup-location>/artifact-alley-YYYYMMDD.dump'
unset PGPASSFILE
```

The backup user needs read access and sequence visibility, not schema mutation privileges. Encrypt backups at rest and in transit. Never store dumps, password files, complete connection URLs, or decrypted archives in this repository or a container image.

## Restore drill

Run at least monthly and before a risky migration:

1. Create or select an authorized disposable PostgreSQL database isolated from production. Do not reuse the live database.
2. Restore the selected dump with a restricted credential: `pg_restore --exit-on-error --no-owner --no-acl --dbname='<disposable-database-name>' '<backup-file>'` while connection details come from `PGHOST`, `PGPORT`, `PGUSER`, and `PGPASSFILE`.
3. Point a disposable application instance at the restored database. Do not enable automatic Flyway baseline. If this is an established Flyway database, run `mvn -Dflyway.url='<credential-free-placeholder>' flyway:validate` only from an approved operator environment or simply start the matching packaged application and require Flyway plus Hibernate validation to pass.
4. Compare table and relationship counts for users, artifacts, bids, artifact images, login attempts, seller/reviewer/winning-bid foreign keys, and the Flyway history. Check sequence values exceed current IDs.
5. Exercise read-only home, search, detail, JSON/XML API, login, health, and image checks. Use disposable accounts if a write-path smoke check is authorized.
6. Record the achieved recovery point and recovery time, then destroy the disposable resources according to provider policy.

A dump file existing is not proof of recovery. Only a completed restore and validation drill counts.

## Production recovery

Stop writers or put the service in maintenance mode. Choose a provider restore point or logical backup from before the incident. Restore into a new database, validate it as above, and preserve the damaged database for investigation. Switch the existing Render service to newly issued credentials only after validation. Deploy the last application commit compatible with the restored Flyway version. Do not delete rows from `flyway_schema_history`, run destructive down migrations, or restore over the only production copy.

For an accidental migration, prefer a reviewed forward fix. Application rollback is safe only when the current schema remains compatible with the old application. Otherwise restore the paired database backup and application image together.

## Cloudinary assets

Confirm retention, backup, versioning, export, and deletion behavior in the active Cloudinary plan; none was assumed here. Keep a protected metadata export containing image row ID, artifact ID, storage key, content type, cover flag, order, and timestamp with each database backup. It contains no provider secret.

Reconcile regularly:

- A database row whose remote object is missing should show the existing placeholder/error behavior. Recover the object from provider backup or the retained local migration original, using the same database storage key when possible. Do not accept a user-supplied remote URL.
- A provider object without a database row is an orphan. Quarantine/report it first, confirm it is outside any in-flight transaction and retention window, then delete it through an authenticated operator tool.
- Count differences alone are insufficient because failed transaction cleanup can create an orphan and multiple images belong to one artifact. Compare identifiers and sample decoded content.

For a restore drill, use a nonproduction Cloudinary folder or account authorized for testing. Upload representative JPEG and PNG files, restore matching metadata to the disposable database, verify HTTPS reads, cover/order behavior and deletion, then remove only the nonproduction test assets. Ordinary automated tests use a local mock provider and never contact the real account.
