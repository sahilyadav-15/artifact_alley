<%@ page contentType="text/html;charset=UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
    <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Artifact Alley | Live Auctions</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="/css/site.css" rel="stylesheet">
        </head>

        <body>
            <nav class="navbar navbar-expand-lg navbar-dark auction-nav">
                <div class="container">
                    <a class="navbar-brand" href="/">Artifact Alley</a>
                    <div class="d-flex align-items-center gap-3">
                        <span class="navbar-text d-none d-md-inline">Artifacts &amp; Antiques</span>
                        <c:choose>
                            <c:when test="${not empty sessionScope.signedInUser}">
                                <span class="navbar-text"><c:out value="${sessionScope.signedInUser.name}" />
                                    <small>(<c:out value="${sessionScope.signedInUser.role}" />)</small></span>
                                <c:if test="${sessionScope.signedInUser.role == 'BIDDER'}">
                                    <a class="btn btn-outline-light btn-sm" href="/account/bids">My bids</a>
                                    <form action="/account/become-seller" method="post" class="m-0"><input type="hidden" name="_csrf" value="${csrfToken}"><button
                                            class="btn btn-accent btn-sm" type="submit">Become a seller</button></form>
                                </c:if>
                                <c:if test="${sessionScope.signedInUser.role == 'SELLER'}"><a
                                        class="btn btn-outline-light btn-sm" href="/seller/artifacts">My artifacts</a><a
                                        class="btn btn-accent btn-sm" href="/seller/artifacts/new">Submit artifact</a>
                                </c:if>
                                <c:if test="${sessionScope.signedInUser.role == 'ADMIN'}"><a
                                        class="btn btn-accent btn-sm" href="/admin/artifacts/pending">Pending
                                        approvals</a></c:if>
                                <a class="btn btn-outline-light btn-sm" href="/account/password">Change password</a>
                                <form action="/logout" method="post" class="m-0" id="logout-form"><input type="hidden" name="_csrf" value="${csrfToken}"><button
                                        class="btn btn-outline-light btn-sm" type="button" id="logout-trigger">Log
                                        out</button></form>
                            </c:when>
                            <c:otherwise>
                                <a class="btn btn-outline-light btn-sm" href="/login">Sign in</a>
                                <a class="btn btn-accent btn-sm" href="/register">Register</a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </nav>
            <div class="logout-modal" id="logout-modal" hidden>
                <section class="logout-dialog" role="dialog" aria-modal="true" aria-labelledby="logout-title"
                    aria-describedby="logout-message">
                    <h2 id="logout-title">Log out?</h2>
                    <p id="logout-message">Are you sure you want to log out of Artifact Alley?</p>
                    <div class="logout-actions"><button class="btn btn-outline-secondary" type="button"
                            id="logout-cancel">Cancel</button><button class="btn btn-primary" type="button"
                            id="logout-confirm">Log out</button></div>
                </section>
            </div>
            <main>
                <section class="hero py-5">
                    <div class="container py-4">
                        <p class="eyebrow">CURATED HISTORY, OPEN BIDDING</p>
                        <h1>Discover an object with a story.</h1>
                        <p class="lead">Browse verified artifacts and place your bid before the auction closes.</p>
                    </div>
                </section>
                <section class="container py-5" aria-labelledby="auctions-heading">
                    <c:if test="${not empty successMessage}">
                        <div class="alert alert-success" role="status"><c:out value="${successMessage}" /></div>
                    </c:if>
                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></div>
                    </c:if>
                    <form class="discovery-form mb-4" method="get" action="/" role="search" aria-label="Search live auctions">
                        <div class="row g-3">
                            <div class="col-12 col-lg-6">
                                <label class="form-label" for="search-q">Search artifacts</label>
                                <input class="form-control" id="search-q" name="q" type="search" maxlength="100"
                                    value="<c:out value='${searchRequest.query}' />" placeholder="Title, description, era or category"
                                    <c:if test="${invalidField == 'q'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                            </div>
                            <div class="col-6 col-lg-3">
                                <label class="form-label" for="search-category">Category</label>
                                <select class="form-select" id="search-category" name="category"
                                    <c:if test="${invalidField == 'category'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                                    <option value="">All categories</option>
                                    <c:forEach items="${categories}" var="option"><option value="${option}" <c:if test="${fn:toUpperCase(searchRequest.category) == option}">selected</c:if>><c:out value="${option}" /></option></c:forEach>
                                </select>
                            </div>
                            <div class="col-6 col-lg-3">
                                <label class="form-label" for="search-era">Era</label>
                                <input class="form-control" id="search-era" name="era" maxlength="60"
                                    value="<c:out value='${searchRequest.era}' />" placeholder="e.g. 18th century"
                                    <c:if test="${invalidField == 'era'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label" for="search-min-price">Minimum price (₹)</label>
                                <input class="form-control" id="search-min-price" name="minPrice" type="number" min="0" step="0.01" inputmode="decimal"
                                    value="<c:out value='${searchRequest.minimumPrice}' />"
                                    <c:if test="${invalidField == 'minPrice'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label" for="search-max-price">Maximum price (₹)</label>
                                <input class="form-control" id="search-max-price" name="maxPrice" type="number" min="0" step="0.01" inputmode="decimal"
                                    value="<c:out value='${searchRequest.maximumPrice}' />"
                                    <c:if test="${invalidField == 'maxPrice'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label" for="search-ending">Closing within</label>
                                <select class="form-select" id="search-ending" name="endingWithin"
                                    <c:if test="${invalidField == 'endingWithin'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                                    <option value="">Any time</option>
                                    <option value="24h" <c:if test="${searchRequest.endingWithin == '24h'}">selected</c:if>>24 hours</option>
                                    <option value="3d" <c:if test="${searchRequest.endingWithin == '3d'}">selected</c:if>>3 days</option>
                                    <option value="7d" <c:if test="${searchRequest.endingWithin == '7d'}">selected</c:if>>7 days</option>
                                </select>
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label" for="search-sort">Sort by</label>
                                <select class="form-select" id="search-sort" name="sort"
                                    <c:if test="${invalidField == 'sort'}">aria-invalid="true" aria-describedby="search-error"</c:if>>
                                    <option value="ending-soon" <c:if test="${empty searchRequest.sort || searchRequest.sort == 'ending-soon'}">selected</c:if>>Ending soon</option>
                                    <option value="newest" <c:if test="${searchRequest.sort == 'newest'}">selected</c:if>>Newly listed</option>
                                    <option value="price-asc" <c:if test="${searchRequest.sort == 'price-asc'}">selected</c:if>>Price: low to high</option>
                                    <option value="price-desc" <c:if test="${searchRequest.sort == 'price-desc'}">selected</c:if>>Price: high to low</option>
                                    <option value="most-bids" <c:if test="${searchRequest.sort == 'most-bids'}">selected</c:if>>Most bids</option>
                                </select>
                            </div>
                            <div class="col-6 col-md-3">
                                <label class="form-label" for="search-size">Items per page</label>
                                <select class="form-select" id="search-size" name="size">
                                    <option value="12" <c:if test="${empty searchRequest.size || searchRequest.size == '12'}">selected</c:if>>12</option>
                                    <option value="24" <c:if test="${searchRequest.size == '24'}">selected</c:if>>24</option>
                                    <option value="48" <c:if test="${searchRequest.size == '48'}">selected</c:if>>48</option>
                                </select>
                            </div>
                            <div class="col-12 col-md-9 d-flex align-items-end gap-2">
                                <button class="btn btn-primary" type="submit">Search auctions</button>
                                <a class="btn btn-outline-secondary" href="/">Clear all</a>
                            </div>
                        </div>
                    </form>
                    <c:if test="${not empty searchError}"><div class="alert alert-danger" id="search-error" role="alert"><c:out value="${searchError}" /></div></c:if>
                    <c:if test="${searchResult.filtered}">
                        <div class="active-filters mb-4" aria-label="Active filters">
                            <span class="active-filters-label">Active filters:</span>
                            <c:forEach items="${searchResult.activeFilters}" var="filter">
                                <a class="filter-chip" href="<c:out value='${filter.removeUrl}' />" aria-label="Remove ${fn:escapeXml(filter.label)} filter: ${fn:escapeXml(filter.value)}"><c:out value="${filter.label}" />: <c:out value="${filter.value}" /> <span aria-hidden="true">×</span></a>
                            </c:forEach>
                            <a class="filter-clear" href="/">Clear all</a>
                        </div>
                    </c:if>
                    <div class="d-flex justify-content-between align-items-end mb-4 gap-3">
                        <div>
                            <p class="eyebrow text-dark">AVAILABLE NOW</p>
                            <h2 id="auctions-heading">Live auctions</h2>
                        </div><span class="text-muted" role="status" aria-live="polite"><c:out value="${searchResult.totalItems}" /> matching items · Page <c:out value="${searchResult.currentPage}" /><c:if test="${searchResult.totalPages > 0}"> of <c:out value="${searchResult.totalPages}" /></c:if></span>
                    </div>
                    <c:choose>
                        <c:when test="${empty searchResult.items}">
                            <div class="empty-state discovery-empty" role="status">
                                <c:choose>
                                    <c:when test="${searchResult.totalItems > 0}"><h3>This page has no results</h3><p>Choose an earlier page to continue browsing.</p></c:when>
                                    <c:when test="${searchResult.filtered}"><h3>No auctions match these filters</h3><p>Try removing a filter or using a broader search.</p></c:when>
                                    <c:otherwise><h3>No live auctions are available</h3><p>Please check again later.</p></c:otherwise>
                                </c:choose>
                                <a class="btn btn-primary" href="/">Clear filters</a>
                            </div>
                        </c:when>
                        <c:otherwise><div class="row g-4">
                        <c:forEach items="${searchResult.items}" var="artifact">
                            <div class="col-md-6 col-lg-4">
                                <article class="artifact-card h-100 p-4">
                                    <c:choose><c:when test="${not empty artifact.coverImageUrl}"><img class="artifact-cover mb-3" src="<c:out value='${artifact.coverImageUrl}' />" alt="Photo of ${fn:escapeXml(artifact.title)}"></c:when><c:otherwise><div class="artifact-placeholder mb-3" role="img" aria-label="No photo available for ${fn:escapeXml(artifact.title)}">No photo available</div></c:otherwise></c:choose>
                                    <span class="badge text-bg-light mb-3"><c:out value="${artifact.category}" /></span>
                                    <h3><c:out value="${artifact.title}" /></h3>
                                    <p class="text-muted mb-2"><c:out value="${artifact.era}" /></p>
                                    <p class="description"><c:out value="${artifact.description}" /></p>
                                    <div class="auction-card-meta mt-auto"><span><small>Current price</small><strong>₹ <c:out value="${artifact.currentPrice}" /></strong></span><span><small>Bids</small><strong><c:out value="${artifact.bidCount}" /></strong></span></div>
                                    <div class="border-top pt-3"><time datetime="<c:out value='${artifact.closesAt}' />"><c:out value="${artifact.endsIn}" /></time>
                                        <a class="btn btn-primary w-100 mt-3" href="/artifacts/${artifact.id}">View <c:out value="${artifact.title}" /></a>
                                    </div>
                                </article>
                            </div>
                        </c:forEach>
                    </div></c:otherwise></c:choose>
                    <c:if test="${searchResult.totalPages > 1}">
                        <nav class="mt-5" aria-label="Auction result pages"><ul class="pagination justify-content-center flex-wrap">
                            <li class="page-item ${empty searchResult.previousUrl ? 'disabled' : ''}"><c:choose><c:when test="${empty searchResult.previousUrl}"><span class="page-link" aria-disabled="true">Previous</span></c:when><c:otherwise><a class="page-link" href="<c:out value='${searchResult.previousUrl}' />">Previous</a></c:otherwise></c:choose></li>
                            <c:forEach items="${searchResult.pageLinks}" var="pageLink"><li class="page-item ${pageLink.current ? 'active' : ''}"><a class="page-link" href="<c:out value='${pageLink.url}' />" <c:if test="${pageLink.current}">aria-current="page"</c:if>><c:out value="${pageLink.number}" /></a></li></c:forEach>
                            <li class="page-item ${empty searchResult.nextUrl ? 'disabled' : ''}"><c:choose><c:when test="${empty searchResult.nextUrl}"><span class="page-link" aria-disabled="true">Next</span></c:when><c:otherwise><a class="page-link" href="<c:out value='${searchResult.nextUrl}' />">Next</a></c:otherwise></c:choose></li>
                        </ul></nav>
                    </c:if>
                </section>
            </main>
            <script nonce="${cspNonce}">
                (() => {
                    const trigger = document.getElementById('logout-trigger');
                    if (!trigger) return;
                    const modal = document.getElementById('logout-modal');
                    const cancel = document.getElementById('logout-cancel');
                    const confirm = document.getElementById('logout-confirm');
                    const form = document.getElementById('logout-form');
                    const close = () => { modal.hidden = true; trigger.focus(); };
                    trigger.addEventListener('click', () => { modal.hidden = false; cancel.focus(); });
                    cancel.addEventListener('click', close);
                    confirm.addEventListener('click', () => form.submit());
                    modal.addEventListener('click', event => { if (event.target === modal) close(); });
                    document.addEventListener('keydown', event => { if (event.key === 'Escape' && !modal.hidden) close(); });
                })();
            </script>
        </body>

        </html>
