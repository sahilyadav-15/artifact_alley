<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${details.artifact.title}" /> | Artifact Alley</title>
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
                    <c:if test="${sessionScope.signedInUser.role == 'BIDDER'}"><a class="btn btn-outline-light btn-sm" href="/account/bids">My bids</a><form action="/account/become-seller" method="post" class="m-0"><button class="btn btn-accent btn-sm" type="submit">Become a seller</button></form></c:if>
                    <c:if test="${sessionScope.signedInUser.role == 'SELLER'}"><a class="btn btn-accent btn-sm" href="/seller/artifacts/new">Submit artifact</a></c:if>
                    <c:if test="${sessionScope.signedInUser.role == 'ADMIN'}"><a class="btn btn-accent btn-sm" href="/admin/artifacts/pending">Pending approvals</a></c:if>
                    <a class="btn btn-outline-light btn-sm" href="/account/password">Change password</a>
                    <form action="/logout" method="post" class="m-0" id="logout-form"><button class="btn btn-outline-light btn-sm" type="button" id="logout-trigger">Log out</button></form>
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
    <section class="logout-dialog" role="dialog" aria-modal="true" aria-labelledby="logout-title" aria-describedby="logout-message">
        <h2 id="logout-title">Log out?</h2>
        <p id="logout-message">Are you sure you want to log out of Artifact Alley?</p>
        <div class="logout-actions"><button class="btn btn-outline-secondary" type="button" id="logout-cancel">Cancel</button><button class="btn btn-primary" type="button" id="logout-confirm">Log out</button></div>
    </section>
</div>
<main class="container py-5">
    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}" /></div></c:if>
    <div class="row g-4">
        <div class="col-lg-7">
            <article class="artifact-detail-card p-4 p-md-5">
                <div class="image-gallery mb-4"><c:choose><c:when test="${empty artifactImages}"><div class="artifact-placeholder" role="img" aria-label="No photo available for ${fn:escapeXml(details.artifact.title)}">No photo available</div></c:when><c:otherwise><c:forEach items="${artifactImages}" var="image"><figure><img src="${imageUrls[image.id]}" alt="${fn:escapeXml(details.artifact.title)}"><c:if test="${image.coverImage}"><figcaption>Cover photo</figcaption></c:if></figure></c:forEach></c:otherwise></c:choose></div>
                <div class="d-flex flex-wrap justify-content-between gap-2 mb-3">
                    <span class="badge text-bg-light"><c:out value="${details.artifact.category}" /></span>
                    <span class="auction-state"><c:out value="${auctionState}" /></span>
                </div>
                <h1><c:out value="${details.artifact.title}" /></h1>
                <p class="text-muted"><c:out value="${details.artifact.era}" /></p>
                <p class="description detail-description"><c:out value="${details.artifact.description}" /></p>
                <c:if test="${details.artifact.status == 'SOLD'}">
                    <div class="auction-outcome" role="status">
                        <strong>Auction ended — Sold</strong>
                        <c:choose><c:when test="${signedInBidderWon}"><span>You won this auction.</span></c:when><c:otherwise><span>Winning bidder: <c:out value="${details.winnerDisplayName}" /></span></c:otherwise></c:choose>
                    </div>
                </c:if>
                <c:if test="${details.artifact.status == 'CLOSED'}"><div class="auction-outcome" role="status"><strong>Auction closed</strong><span>No bids were placed.</span></div></c:if>
                <dl class="auction-facts">
                    <div><dt>Starting price</dt><dd>₹ <c:out value="${details.artifact.startingPrice}" /></dd></div>
                    <div><dt><c:choose><c:when test="${details.artifact.status == 'SOLD'}">Final price</c:when><c:otherwise>Current price</c:otherwise></c:choose></dt><dd>₹ <c:out value="${details.artifact.currentPrice}" /></dd></div>
                    <c:if test="${auctionOpen}"><div><dt>Minimum next bid</dt><dd>₹ <c:out value="${details.minimumNextBid}" /></dd></div></c:if>
                    <div><dt>Closes</dt><dd><c:out value="${details.closesAtDisplay}" /></dd></div>
                    <div><dt>Auction state</dt><dd><c:out value="${auctionState}" /></dd></div>
                    <div><dt>Bid count</dt><dd><c:out value="${details.bidCount}" /></dd></div>
                    <c:if test="${not empty details.settledAtDisplay}"><div><dt>Settled</dt><dd><c:out value="${details.settledAtDisplay}" /></dd></div></c:if>
                </dl>
            </article>
        </div>
        <div class="col-lg-5">
            <section class="bid-panel p-4" aria-labelledby="bid-title">
                <h2 id="bid-title">Place a bid</h2>
                <c:if test="${not empty bidError}"><div class="alert alert-danger" role="alert"><c:out value="${bidError}" /></div></c:if>
                <c:choose>
                    <c:when test="${canBid}">
                        <p class="description">Enter ₹<c:out value="${details.minimumNextBid}" /> or more. The latest price is checked again when you submit.</p>
                        <form:form method="post" action="/artifacts/${details.artifact.id}/bids" modelAttribute="bidForm" novalidate="true">
                            <form:label path="amount" cssClass="form-label">Your bid amount (₹)</form:label>
                            <form:input path="amount" type="number" cssClass="form-control" min="0.01" step="0.01" inputmode="decimal" />
                            <form:errors path="amount" cssClass="invalid-feedback d-block" />
                            <button class="btn btn-primary w-100 mt-3" type="submit">Place bid</button>
                        </form:form>
                    </c:when>
                    <c:when test="${details.artifact.status == 'SOLD'}"><c:choose><c:when test="${signedInBidderWon}"><div class="winner-message" role="status">You won this auction.</div></c:when><c:otherwise><p class="description">This auction has ended.</p></c:otherwise></c:choose></c:when>
                    <c:when test="${details.artifact.status == 'CLOSED'}"><p class="description">This auction closed without bids.</p></c:when>
                    <c:when test="${!auctionOpen}"><p class="description">Bidding is unavailable because this auction is not live.</p></c:when>
                    <c:when test="${empty sessionScope.signedInUser}"><p class="description">Sign in with a bidder account to take part in this auction.</p><a class="btn btn-primary" href="/login">Sign in to bid</a></c:when>
                    <c:otherwise><p class="description">Seller and administrator accounts may view auctions but cannot place bids.</p></c:otherwise>
                </c:choose>
            </section>
        </div>
    </div>
    <section class="bid-history mt-5" aria-labelledby="history-title">
        <div class="d-flex justify-content-between align-items-end mb-3"><h2 id="history-title">Bid history</h2><span class="text-muted"><c:out value="${details.bidCount}" /> bids</span></div>
        <c:choose>
            <c:when test="${empty details.bidHistory}"><p class="description">No bids have been placed yet.</p></c:when>
            <c:otherwise>
                <div class="table-responsive"><table class="table align-middle"><thead><tr><th scope="col">Bidder</th><th scope="col">Amount</th><th scope="col">Placed</th></tr></thead><tbody>
                    <c:forEach items="${details.bidHistory}" var="bid"><tr><td><c:out value="${bid.bidderDisplayName}" /></td><td>₹ <c:out value="${bid.amount}" /></td><td><c:out value="${bid.placedAtDisplay}" /></td></tr></c:forEach>
                </tbody></table></div>
            </c:otherwise>
        </c:choose>
    </section>
</main>
<script>
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
