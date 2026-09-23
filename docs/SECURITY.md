# Security model

Artifact Alley is a teaching application that uses a plain Servlet `HttpSession`; it does not use Spring Security and is not a security certification. The controls below cover the risks most relevant to its auction workflows. Production still requires HTTPS, secret management, database backups, monitoring, dependency patching, and infrastructure controls.

## Threat model and controls

| Risk | Existing defense | Step 6 defense | Code and tests | Remaining limitation |
| --- | --- | --- | --- | --- |
| Session theft | Server-side session stores only a password-free user summary | 30-minute timeout; `HttpOnly`, `SameSite=Lax`, path `/`; production `Secure`; private-page `no-store` | properties, `SecurityRequestFilter`, `SecurityIntegrationTest` | A stolen live cookie remains usable until expiry/logout; HTTPS is an operator responsibility. |
| Session fixation | Logout invalidated the session | Servlet `changeSessionId()` after login, registration and bidder-to-seller upgrade; CSRF rotates at each boundary | `AuthController`, `SecurityIntegrationTest` | No device/session management UI. |
| CSRF | POST was used for mutations | Per-session 256-bit random token; one filter validates every unsafe browser and API request; API uses `X-CSRF-Token` | `CsrfTokenService`, `SecurityRequestFilter`, all JSPs, `SecurityIntegrationTest` | No endpoints are exempt. Clients must keep the cookie and token together. |
| Brute-force login | PBKDF2 made guesses expensive | Database-persisted, hashed normalized email plus remote-address key; five failures/15 minutes by default; success clears state | `LoginAttempt`, `LoginThrottleService`, `LoginThrottleServiceTest` | Distributed attacks across many source addresses still require edge rate limiting. The app intentionally ignores `X-Forwarded-For`. |
| User enumeration | Login uses one message for unknown email and wrong password | Throttling message is generic; audit logs omit email and credentials | `AuthController`, security tests | Registration still reports duplicate email as an intentional usability tradeoff. |
| Stored/reflected XSS | Most JSP text already used JSTL escaping | JSP audit moved remaining input-derived text to `c:out`; CSP uses a random response nonce and disallows inline event handlers/eval | JSPs, `SecurityRequestFilter`, source audit | Bootstrap CSS is loaded from the declared jsDelivr origin; style policy allows inline styles for Bootstrap/JSP compatibility. |
| IDOR and unauthorized actions | Seller, bidding and review services reload users and enforce ownership/roles | API role resolution now reloads current database role; stale session values are refreshed/removed | `SessionAuthentication`, services, `SecurityIntegrationTest`, management/REST tests | Browser controllers retain simple redirect logic, with authoritative checks repeated in services. |
| Malicious uploads/path traversal | Five-file/5 MB limits, decode and re-encode JPEG/PNG, UUID storage keys, normalized paths, transaction cleanup | Re-audited; `nosniff` applies to served images and mutations require CSRF plus seller ownership | `ArtifactImageService`, `LocalArtifactImageStorage`, image tests | Local/Render filesystem is not durable and has no malware scanner. |
| SQL injection | JPA/JPQL and prepared JDBC values are bound | Discovery wildcards are escaped, values and pagination are bound, sort order is a closed allowlist | search/report repositories and integration tests | Dynamic schema/identifier selection is intentionally unsupported. |
| Mass assignment | Purpose-specific form/API DTOs; entities are not request bodies | Audit confirmed IDs, roles, ownership, status, prices, winner/review/storage fields are server assigned | form/API DTOs, REST tests | New DTO fields require the same review. |
| Concurrent auction changes | Transactional writes and pessimistic artifact row locks | Audit events added after accepted bid, settlement and withdrawal paths | bidding/settlement/seller services and concurrency tests | Correct cross-instance locking depends on the production database; PostgreSQL was not automatically validated unless its optional environment is run. |
| Sensitive logs/errors | Structured API errors omit stack traces and entities | Correlation ID on responses; security audit records event/user/target/result only; tokens, cookies, passwords, bodies and session IDs are excluded | `SecurityAuditService`, `RestApiExceptionHandler` | Application/platform log retention is an operator policy. |
| Insecure production defaults | Production reads database/admin secrets from environment and disables H2 console | Startup validates PostgreSQL, Flyway, HTTPS origin, secure cookie/proxy settings, upload limits and Cloudinary; Swagger is disabled in production | production/local properties, `ProductionConfigurationValidator`, validator tests | Operator access to Render, Neon and Cloudinary remains outside application control. |

## Authentication and authorization

`signedInUser` is a small session value containing user ID, display fields and the last observed role. It is never treated as authoritative for an important mutation. `SessionAuthentication` and the domain services load the current `User` row by ID; API role failures are JSON/XML 401 or 403 responses and never redirects. Browser guests receive a safe login redirect, wrong roles receive a safe redirect or controlled page, and seller ownership failures intentionally appear as a controlled not-found result so sequential IDs do not disclose private listings.

Passwords remain 6–72 characters to preserve the established project requirement and are stored with salted PBKDF2-HMAC-SHA-256 at 210,000 iterations. Production operators should require stronger user passwords through policy. The local bootstrap administrator password is only for local development. `INITIAL_ADMIN_PASSWORD` must be nonblank and cannot be the known local default under the `prod` profile.

## Sessions, cookies, and CSRF

The server uses one standard `JSESSIONID`; it never stores identity in a custom cookie. The session ID and CSRF token both rotate after successful login, automatic sign-in registration, and role upgrade. Failed authentication does not authenticate or rotate the caller into a privileged state. Logout requires CSRF and invalidates the entire session.

Production cookie settings are `HttpOnly`, `Secure`, `SameSite=Lax`, and path `/`. Local development disables only `Secure` so HTTP localhost works. Production must terminate HTTPS before the application; otherwise a Secure cookie will correctly not travel over HTTP. No cookie domain is configured.

Browser POST forms include `_csrf`. REST clients first call `GET /api/v1/session/csrf` while retaining the session cookie, then send the returned token as `X-CSRF-Token` on POST, PUT, PATCH, and DELETE. Login is a browser form and therefore needs the token obtained before login. Authentication rotates it, so API clients fetch it again after login. Tokens never belong in URLs or logs. Safe GET/HEAD requests do not require them.

In Swagger UI, call the CSRF endpoint first, sign in through the normal `/login` page in the same browser, call the CSRF endpoint again, and supply the token in the documented header. The Postman collection automates capturing the token before and after login; its collection variable starts empty and contains no committed secret.

## Headers and content handling

Every response receives `X-Content-Type-Options: nosniff`, a strict referrer policy, permissions restrictions, frame denial, and CSP. Application scripts carry a fresh response nonce; `unsafe-eval` and script `unsafe-inline` are absent. Authenticated account, seller/admin API, login, registration, and token responses use `Cache-Control: no-store`. The local-only H2 console receives a separate same-origin frame/CSP policy without weakening production routes.

Unexpected REST failures return a request ID and generic message; the stack trace stays in server logs associated with that ID. Audit events cover login success/failure/throttling, logout, password change, role upgrade, accepted bids, approval, rejection, withdrawal, and settlement. Records contain no credentials, request body, email, token, cookie, session ID, database URL, or image bytes.

## Query, upload, and request boundaries

Search and reports use parameter binding. Search `%`, `_`, and the escape character are treated literally; sort names map to fixed SQL fragments. Direct JDBC demonstrations use `PreparedStatement`. REST and form requests bind to purpose-specific objects, never JPA entities, so clients cannot assign IDs, roles, owners, state, price, winning bid, timestamps, password hashes, or storage paths.

Image input is limited to five files, 5 MB each and 26 MB per multipart request. The server verifies actual decodable JPEG/PNG content, re-encodes it, chooses a random filename, normalizes the path, and compensates filesystem writes if database work rolls back. Tests cover traversal-style names, absolute/double-extension names, empty/corrupt/renamed HTML, oversized input, excess count, authorization and cleanup.

## Test strategy and limitations

The normal suite uses isolated in-memory H2, fixed clocks, MockMvc sessions, temporary image directories, controller slices, service tests, transaction/concurrency tests, and packaged-WAR checks. Security integration tests cover session and CSRF rotation, logout invalidation, browser/API rejection shapes, cross-session tokens, security headers, and database-authoritative roles. Throttle tests cover threshold, expiry, normalization, successful clearing, client separation, bounded hashes, and cleanup without sleeps.

The optional Testcontainers class is included in the ordinary build and skips clearly when it cannot negotiate a usable Docker endpoint. It covers PostgreSQL Flyway/schema startup, search, JDBC reporting, persistent throttling, competing bids, and withdrawal-versus-bid locking when available. A skipped result is not PostgreSQL validation; H2 results alone do not prove PostgreSQL locking/schema behavior. Actuator exposes only health and info, hides details, and keeps metrics private. Cloudinary uses server-side signed operations, random identifiers, HTTPS delivery and timeouts. The application also lacks MFA, breached-password checking, centralized session revocation, a persistent audit store, malware scanning, WAF/rate limiting, automated key rotation, and formal penetration testing.
