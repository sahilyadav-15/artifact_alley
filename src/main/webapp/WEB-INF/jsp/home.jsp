<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
                    <span class="navbar-text">${sessionScope.signedInUser.name} <small>(${sessionScope.signedInUser.role})</small></span>
                    <c:if test="${sessionScope.signedInUser.role == 'BIDDER'}"><form action="/account/become-seller" method="post" class="m-0"><button class="btn btn-accent btn-sm" type="submit">Become a seller</button></form></c:if>
                    <c:if test="${sessionScope.signedInUser.role == 'SELLER'}"><a class="btn btn-accent btn-sm" href="/seller/artifacts/new">Submit artifact</a></c:if>
                    <c:if test="${sessionScope.signedInUser.role == 'ADMIN'}"><a class="btn btn-accent btn-sm" href="/admin/artifacts/pending">Pending approvals</a></c:if>
                    <a class="btn btn-outline-light btn-sm" href="/account/password">Change password</a>
                    <form action="/logout" method="post" class="m-0"><button class="btn btn-outline-light btn-sm" type="submit">Log out</button></form>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-outline-light btn-sm" href="/login">Sign in</a>
                    <a class="btn btn-accent btn-sm" href="/register">Register</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</nav>
<main>
    <section class="hero py-5"><div class="container py-4">
        <p class="eyebrow">CURATED HISTORY, OPEN BIDDING</p>
        <h1>Discover an object with a story.</h1>
        <p class="lead">Browse verified artifacts and place your bid before the auction closes.</p>
    </div></section>
    <section class="container py-5">
        <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status">${successMessage}</div></c:if>
        <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert">${errorMessage}</div></c:if>
        <div class="d-flex justify-content-between align-items-end mb-4"><div><p class="eyebrow text-dark">AVAILABLE NOW</p><h2>Live auctions</h2></div><span class="text-muted">${artifacts.size()} items</span></div>
        <div class="row g-4">
            <c:forEach items="${artifacts}" var="artifact">
                <div class="col-md-6 col-lg-4"><article class="artifact-card h-100 p-4">
                    <span class="badge text-bg-light mb-3">${artifact.category}</span>
                    <h3>${artifact.title}</h3><p class="text-muted mb-4">${artifact.era}</p>
                    <p class="description">${artifact.description}</p>
                    <div class="border-top pt-3 mt-auto"><small class="text-muted d-block">Current bid</small><strong>₹ ${artifact.currentPrice}</strong></div>
                </article></div>
            </c:forEach>
        </div>
    </section>
</main>
</body>
</html>
