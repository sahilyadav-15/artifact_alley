# Syllabus coverage

This document maps `syllabus.md` to working code and states limitations honestly. “Framework” means the behavior is supplied by Spring Boot or the Servlet/JSP container and exercised by this application. “Intentional” means the topic is explained without adding an unsafe or artificial production example.

## Module 1: JDBC and Servlets

| Topic | Status | Evidence | Test or route | Viva explanation |
| --- | --- | --- | --- | --- |
| JDBC introduction and database connectivity | Complete | `pom.xml`, `application-local.properties`, `application-prod.properties` | Application context tests | Boot configures a pooled `DataSource`; H2 is local/test and PostgreSQL is production. Credentials remain external. |
| JDBC driver | Framework | H2 and PostgreSQL runtime dependencies | Context startup | JDBC 4 drivers register through the service-provider mechanism. Manual `Class.forName` is unnecessary. |
| `Connection` | Complete | `ArtifactActivityJdbcRepository` | `JdbcReportingIntegrationTest` | The configured `DataSource` supplies a connection; try-with-resources returns/closes it safely. |
| `Statement` | Intentional | This document | Not applicable | Raw `Statement` is deliberately absent for user-influenced data because concatenated SQL creates injection risk. |
| `PreparedStatement` | Complete | `ArtifactActivityJdbcRepository.findBetween` | `JdbcReportingIntegrationTest` | `?` placeholders are bound as timestamps, keeping data separate from SQL. |
| `ResultSet` | Complete | `ArtifactActivityJdbcRepository.findBetween` | `JdbcReportingIntegrationTest` | The repository advances row by row and maps named columns to a purpose-specific record. |
| Transaction management | Complete | `BiddingService`, `AuctionSettlementService`, `SellerArtifactService` | `BidRollbackIntegrationTest`, auction lifecycle/concurrency tests | Transactions belong at service boundaries. A bid insert and price change commit or roll back together. Settlement similarly stores status, winner, price and time atomically. |
| Stored procedures | Partial/limitation | This document | Not applicable | H2 and PostgreSQL procedure syntax differs. A fake procedure would reduce portability, so aggregate reporting uses portable parameterized SQL. |
| Servlet overview and API | Framework | Spring MVC controllers and `HttpSession` parameters | MVC/controller tests | Spring’s `DispatcherServlet` is the HTTP servlet front controller and controllers use Jakarta Servlet request/session objects where needed. |
| Servlet lifecycle | Framework | Embedded Tomcat + `DispatcherServlet` | Packaged-WAR startup | Tomcat creates, initializes, invokes and destroys servlet instances. Core features stay in Spring MVC rather than a parallel servlet. |
| `GenericServlet` and `HttpServlet` | Framework/conceptual | Servlet container and Spring MVC | Startup | `HttpServlet` adds HTTP methods above `GenericServlet`; `DispatcherServlet` is Spring’s HTTP servlet. |
| RequestDispatcher / view forwarding | Complete | MVC controllers returning JSP view names, `WEB-INF/jsp` | Existing controller tests | A view name is resolved and rendered server-side; protected JSP files cannot be fetched directly. |
| `sendRedirect` | Complete | `AuthController`, bidding and seller web controllers | Existing controller tests | Browser POST actions return `redirect:` for PRG. REST controllers return status codes and never redirect to login. |
| Cookie session tracking | Complete | `AuthController.SIGNED_IN_USER`, `SessionAuthentication`, cookie properties | `SecurityIntegrationTest`, `RestApiIntegrationTest` | Tomcat carries only `JSESSIONID`; authentication rotates it, production marks it Secure/HttpOnly/SameSite and the stored value is a password-free summary. |
| URL rewriting | Intentional | This document | Not applicable | URL-based session IDs can leak through logs, history and referrers, so the application uses cookie-backed sessions. |
| HTTP session | Complete | login/logout, CSRF filter and role-protected web/API controllers | security and REST tests | Login rotates the ID/token, logout requires CSRF then invalidates it, and APIs produce structured 401/403 instead of redirects. |

## Module 2: JSP

| Topic | Status | Evidence | Test or route | Viva explanation |
| --- | --- | --- | --- | --- |
| JSP introduction, lifecycle and JSP-to-servlet conversion | Framework | `src/main/webapp/WEB-INF/jsp` | Packaged-WAR startup | Jasper compiles a JSP into a servlet, initializes it and invokes its service method. |
| JSP scripting elements | Intentional | JSP files | Source inspection | Scriptlets are intentionally avoided; JSTL and EL keep Java logic out of views. |
| JSP implicit objects | Complete | JSP `${pageContext.request...}` and request attributes | Existing MVC flows | JSP exposes request, response, session, application and page context objects. EL accesses only the required values. |
| JSP directives | Complete | JSP taglib/page directives | JSP rendering | Directives configure the page and import JSTL tag libraries at translation time. |
| Expression Language | Complete | JSP `${...}` expressions | JSP rendering | EL reads model/session values without Java scriptlets. |
| JSP exception handling | Partial/framework | Spring MVC exception handlers and validation views | Controller/service tests | Application errors are translated by MVC; REST has its own structured advice. A JSP `errorPage` is unnecessary for the current flows. |
| Servlet–JSP–JDBC integration | Complete | controllers → services/repositories → JSP | Existing web tests | `DispatcherServlet` routes the request, repositories access the database and the controller supplies a model to the JSP. |
| JSP login and registration | Complete | `AuthController`, `UserService`, login/register JSPs | user/controller tests | Validation and password hashing occur server-side, and successful authentication stores the small session identity. |

## Module 3: Spring Framework

| Topic | Status | Evidence | Test or route | Viva explanation |
| --- | --- | --- | --- | --- |
| Spring ecosystem/modules | Complete | MVC, Data JPA, JDBC, validation and transaction dependencies | Full test suite | The application combines focused Spring modules under Boot dependency management. |
| IoC container | Complete | `@SpringBootApplication` and managed components | Context startup | The container creates components and controls their dependencies and lifecycle. |
| Maven setup | Complete | `pom.xml`, Maven wrapper | `mvn clean test package` | Maven resolves dependencies, compiles Java 21 code, runs tests and builds the executable WAR. |
| Constructor injection | Complete/preferred | controllers, services and repositories | Context startup | Required collaborators are explicit and objects can be tested with substitutes. |
| Setter injection | Demonstrated where appropriate | form objects such as `ArtifactSubmissionForm` | validation tests | MVC binds mutable form properties through setters; services use constructor injection. |
| Field injection | Intentional | production source inspection | Not applicable | Production components avoid field injection because it hides required dependencies; some tests use `@Autowired` fields for fixtures. |
| Java configuration | Complete | `AuctionTimeConfig`, `OpenApiConfig` | context/OpenAPI tests | `@Configuration` and `@Bean` define the application clock and OpenAPI model. |
| Component scanning and annotations | Complete | `@Service`, `@Repository`, `@Controller`, `@RestController` | Context startup | Boot scans from `com.artifactalley` and registers annotated classes. |
| Spring JDBC | Complete | `AuctionReportJdbcRepository` with `NamedParameterJdbcTemplate` | admin summary route, `JdbcReportingIntegrationTest` | Spring obtains/releases connections and maps result rows; the query limit is bound rather than concatenated. |

## Module 4: Spring MVC and Spring Boot

| Topic | Status | Evidence | Test or route | Viva explanation |
| --- | --- | --- | --- | --- |
| MVC architecture/controllers | Complete | web and API controller packages | controller and REST tests | DispatcherServlet selects a controller; web controllers return a model/view and REST controllers use message converters. |
| Passing controller data to a view | Complete | existing `Model` attributes and JSP EL | existing controller tests | Controllers load data through services and expose only view-ready values. |
| JSP in Spring MVC | Complete | view prefix/suffix and JSP files | packaged-WAR startup | JSPs live under `WEB-INF` and are resolved by logical view name. |
| MVC with Spring JDBC | Complete | `AdminArtifactApiController` → `AuctionReportJdbcRepository` | `GET /api/v1/admin/reports/auction-summary` | A role-protected MVC route delegates reporting SQL to a repository. |
| Spring Boot introduction / Initializer-style structure | Complete | `ArtifactAlleyApplication`, conventional `src/main` layout | startup | Boot starts an embedded container and applies sensible defaults from classpath dependencies. |
| Boot annotations and auto-configuration | Complete | `@SpringBootApplication`, properties | Context startup | Auto-configuration supplies MVC, Jackson, JPA, JDBC, Hikari and validation infrastructure. |
| Data JPA and H2 | Complete | entity/repository packages and local/test properties | integration tests | H2 provides local/test persistence; `JpaRepository` supplies CRUD and custom queries add locks. |
| HTML/static content | Complete | JSPs and `src/main/resources/static` | browser routes | MVC renders HTML and Boot serves public static assets. |
| HTTP methods | Complete | GET/POST/PUT mappings | `RestApiIntegrationTest` | Reads use GET, creation/actions use POST, draft replacement uses PUT and withdrawal returns 204. |
| Project Lombok | Intentional | Source inspection | compilation | Lombok is not required; explicit constructors/accessors make the educational code and generated API boundaries visible. |

## Module 5: JPA and REST

| Topic | Status | Evidence | Test or route | Viva explanation |
| --- | --- | --- | --- | --- |
| ORM/JPA and annotations | Complete | `Artifact`, `Bid`, `User`, `ArtifactImage` | repository/integration tests | Entities map object state to relational rows while DTOs protect API boundaries. |
| JPA relationships | Complete | entity relationship annotations; audit below | lifecycle/concurrency tests | Relationships are unidirectional where reverse navigation is unnecessary, avoiding recursive graphs. |
| RESTful API and versioning | Complete | `com.artifactalley.api`, `/api/v1` | `RestApiIntegrationTest` | The path carries a major version; JSP and REST discovery reuse `ArtifactSearchService`. |
| CRUD with Spring Data JPA | Complete | repositories and seller API | seller API tests | Create submits, read lists/details, update edits eligible drafts, and delete semantics use withdrawal rather than destructive history deletion. |
| `JpaRepository` / `CrudRepository` | Complete | repository interfaces | integration tests | `JpaRepository` extends CRUD capabilities and adds paging/flush; `JpaSpecificationExecutor` provides safe filtered pages. |
| REST exceptions and HTTP statuses | Complete | `RestApiExceptionHandler`, negotiation handler | REST integration tests | One safe error DTO supports JSON/XML and covers 400/401/403/404/409/415/422/500 classes. |
| JSON/XML | Complete | Jackson XML dependency and API media types | `RestApiIntegrationTest` | The same DTOs serialize through Jackson JSON or XML converters; wrappers keep collections stable. |
| Postman | Complete | `docs/postman` | JSON syntax validation | The credential-free collection covers public, bidder, seller, administrator, JSON and XML requests. |
| Swagger/OpenAPI | Complete | springdoc dependency, `OpenApiConfig`, annotations | `/v3/api-docs`, `/swagger-ui.html`, OpenAPI test | The document contains DTO schemas and a cookie security scheme, never entity/password schemas. |
| Deployment | Complete for project scope | Dockerfile, executable WAR, Flyway, Actuator, Cloudinary storage, CI and production runbook | package/WAR/migration/health/container checks | The same WAR runs with embedded Tomcat locally or as a non-root container; Flyway creates/validates schema and production uses persistent image storage. |

## Relationship and index audit

| Relationship | Cardinality and owner | Fetch | Cascade/orphan removal | Nullability and index decision |
| --- | --- | --- | --- | --- |
| Artifact → seller | many artifacts to one user; `Artifact` owns `seller_id` | lazy | none / none | Nullable for legacy rows; `idx_artifact_seller` supports ownership queries. |
| Bid → artifact | many bids to one artifact; `Bid` owns `artifact_id` | lazy | none / none | Non-null; `idx_bid_artifact_placed` supports history ordering. |
| Bid → bidder | many bids to one user; `Bid` owns `bidder_id` | lazy | none / none | Non-null; `idx_bid_bidder` supports account activity. |
| Artifact → winning bid | many-to-one reference; `Artifact` owns `winning_bid_id` | lazy | none / none | Nullable until a sold settlement; no delete cascade preserves history. |
| Artifact → reviewing administrator | many reviewed artifacts to one user; `Artifact` owns `reviewed_by_id` | lazy | none / none | Nullable before review; no cascade to accounts. |
| Artifact image → artifact | many images to one artifact; `ArtifactImage` owns `artifact_id` | lazy | none / none | Non-null; `idx_artifact_image_order` supports ordered galleries. No unused reverse collection is added. |

Artifact status and closing time use `idx_artifact_status_closes`; `idx_artifact_status_id` supports pending-review scans. Discovery also indexes category, current price and submission time. Its focused JDBC repository binds every filter, escapes `LIKE` wildcards, applies an allowlisted sort clause, returns one bounded page plus one count, and projects bid counts and cover-image IDs in the page query. No relationship cascades deletion into users, bids, images or completed auction history.

## Transaction and locking notes

- `BiddingService.placeBid` locks the artifact, validates against the locked price, inserts the bid and changes current price in one transaction. `BidRollbackIntegrationTest` invokes it inside a failing outer workflow and proves both writes roll back.
- `AuctionSettlementService` locks the same row and assigns status, winning bid, final price and settlement time atomically. Existing lifecycle and service tests cover failures and idempotence.
- `SellerArtifactService.withdraw` locks the row and rechecks closing time and bid count, so a concurrent accepted bid cannot leave a partially valid withdrawal.
- Image bytes live outside the database. `ArtifactImageService` registers compensation cleanup when the database transaction fails and delays destructive file deletion until commit.
- Pessimistic locks serialize competing bids, settlement and live withdrawal checks. Transactions remain in services, never controllers or JSPs.

## Runtime safety

H2 console remains enabled only in `application-local.properties`; production disables it. CORS is not opened. Error payloads omit stack traces and credentials. Public DTOs omit seller identity, review metadata, storage keys and complete bidder identities. Protected routes require the existing `JSESSIONID` session cookie, an authoritative database role, and a CSRF header for unsafe methods. `SecurityRequestFilter` adds CSP, frame, referrer, permission, cache and MIME-sniffing controls. `LoginThrottleService` persists only fixed-size hashes of the normalized identifier and direct remote address. See `docs/SECURITY.md` for the threat model and honest limitations.
