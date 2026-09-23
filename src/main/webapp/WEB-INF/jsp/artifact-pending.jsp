<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Pending approvals | Artifact Alley</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="/css/site.css" rel="stylesheet"></head>
<body><nav class="navbar navbar-dark auction-nav"><div class="container"><a class="navbar-brand" href="/">Artifact Alley</a><span class="navbar-text">Administrator workspace</span></div></nav>
<main class="container py-5">
<c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
<c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>
<div class="d-flex justify-content-between align-items-end mb-4"><div><p class="eyebrow text-dark">ADMINISTRATION</p><h1>Pending artifacts</h1></div><span class="text-muted"><c:out value="${artifacts.size()}"/> awaiting review</span></div>
<c:choose><c:when test="${empty artifacts}"><div class="auth-card p-4"><p class="mb-0">No artifacts are waiting for approval.</p></div></c:when><c:otherwise><div class="row g-4">
<c:forEach items="${artifacts}" var="artifact"><div class="col-md-6"><article class="artifact-card h-100 p-4">
<c:choose><c:when test="${not empty coverImageUrls[artifact.id]}"><img class="artifact-cover mb-3" src="${coverImageUrls[artifact.id]}" alt="${fn:escapeXml(artifact.title)}"></c:when><c:otherwise><div class="artifact-placeholder mb-3">No photo available</div></c:otherwise></c:choose>
<span class="badge text-bg-warning mb-3 align-self-start">PENDING APPROVAL</span><h2 class="h4"><c:out value="${artifact.title}"/></h2><p class="text-muted"><c:out value="${artifact.category}"/> · <c:out value="${artifact.era}"/></p>
<dl class="small"><dt>Seller</dt><dd><c:choose><c:when test="${not empty artifact.seller}"><c:out value="${artifact.seller.name}"/> (<c:out value="${artifact.seller.email}"/>)</c:when><c:otherwise>Ownership needs administrator resolution<c:if test="${not empty artifact.submittedByEmail}"> — legacy email: <c:out value="${artifact.submittedByEmail}"/></c:if></c:otherwise></c:choose></dd><dt>Submitted</dt><dd><c:out value="${artifact.submittedAt}"/></dd><dt>Starting price</dt><dd>₹ <c:out value="${artifact.startingPrice}"/></dd><dt>Closes</dt><dd><c:out value="${artifact.closesAt}"/></dd></dl>
<a class="btn btn-primary mt-auto" href="/admin/artifacts/${artifact.id}">Review listing</a>
</article></div></c:forEach></div></c:otherwise></c:choose></main></body></html>
