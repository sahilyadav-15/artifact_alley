<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Pending approvals | Artifact Alley</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="/css/site.css" rel="stylesheet">
</head><body>
<nav class="navbar navbar-dark auction-nav"><div class="container"><a class="navbar-brand" href="/">Artifact Alley</a><span class="navbar-text">Administrator workspace</span></div></nav>
<main class="container py-5">
    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status">${successMessage}</div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert">${errorMessage}</div></c:if>
    <div class="d-flex justify-content-between align-items-end mb-4"><div><p class="eyebrow text-dark">ADMINISTRATION</p><h1>Pending artifacts</h1></div><span class="text-muted">${artifacts.size()} awaiting review</span></div>
    <c:choose><c:when test="${empty artifacts}"><div class="auth-card p-4"><p class="mb-0">No artifacts are waiting for approval.</p></div></c:when><c:otherwise><div class="row g-4"><c:forEach items="${artifacts}" var="artifact"><div class="col-md-6"><article class="artifact-card h-100 p-4">
        <span class="badge text-bg-warning mb-3">PENDING APPROVAL</span><h2 class="h4">${artifact.title}</h2><p class="text-muted">${artifact.category} · ${artifact.era}</p><p class="description">${artifact.description}</p>
        <dl class="small mb-4"><dt>Submitted by</dt><dd>${artifact.submittedByEmail}</dd><dt>Starting price</dt><dd>₹ ${artifact.startingPrice}</dd><dt>Closes</dt><dd>${artifact.closesAt}</dd></dl>
        <form action="/admin/artifacts/${artifact.id}/approve" method="post" class="mt-auto"><button class="btn btn-primary" type="submit">Approve and publish</button></form>
    </article></div></c:forEach></div></c:otherwise></c:choose>
</main>
</body></html>
