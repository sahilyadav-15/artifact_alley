<%@ page contentType="text/html;charset=UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
            <!DOCTYPE html>
            <html lang="en">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Submit artifact | Artifact Alley</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <link href="/css/site.css" rel="stylesheet">
            </head>

            <body>
                <nav class="navbar navbar-dark auction-nav">
                    <div class="container"><a class="navbar-brand" href="/">Artifact Alley</a><span
                            class="navbar-text">Seller submission</span></div>
                </nav>
                <main class="container py-5">
                    <div class="auth-card mx-auto p-4 p-md-5">
                        <p class="eyebrow text-dark">SELLER WORKSPACE</p>
                        <h1>Submit an artifact</h1>
                        <p class="text-muted">Your listing remains private until an administrator approves it.</p>
                        <form:form method="post" action="/seller/artifacts" modelAttribute="artifactSubmissionForm"
                            class="mt-4" novalidate="true" enctype="multipart/form-data">
                            <input type="hidden" name="_csrf" value="${csrfToken}">
                            <form:errors path="*" cssClass="alert alert-danger d-block" element="div" />
                            <div class="mb-3">
                                <form:label path="title" cssClass="form-label">Title</form:label>
                                <form:input path="title" cssClass="form-control" maxlength="120" />
                                <form:errors path="title" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-3">
                                <form:label path="category" cssClass="form-label">Category</form:label>
                                <form:select path="category" cssClass="form-select">
                                    <form:option value="" label="Choose a category" />
                                    <c:forEach items="${categories}" var="category">
                                        <form:option value="${category}"><c:out value="${category}" /></form:option>
                                    </c:forEach>
                                </form:select>
                                <form:errors path="category" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-3">
                                <form:label path="era" cssClass="form-label">Era or year</form:label>
                                <form:input path="era" cssClass="form-control" maxlength="60"
                                    placeholder="e.g. 19th century" />
                                <form:errors path="era" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-3">
                                <form:label path="startingPrice" cssClass="form-label">Starting price (₹)</form:label>
                                <form:input path="startingPrice" type="number" step="0.01" min="1"
                                    cssClass="form-control" />
                                <form:errors path="startingPrice" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-3">
                                <form:label path="closesAt" cssClass="form-label">Auction closes at</form:label>
                                <form:input path="closesAt" type="datetime-local" cssClass="form-control" />
                                <form:errors path="closesAt" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-4">
                                <form:label path="description" cssClass="form-label">Description</form:label>
                                <form:textarea path="description" cssClass="form-control" rows="5" maxlength="1000" />
                                <form:errors path="description" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-4">
                                <label class="form-label" for="images">Photos <span class="text-muted">(optional, up to 5)</span></label>
                                <input class="form-control" id="images" name="images" type="file" accept="image/jpeg,image/png" multiple>
                                <div class="form-text">JPEG or PNG, maximum 5 MB per image. You can manage cover and order after submission.</div>
                            </div>
                            <button class="btn btn-primary w-100" type="submit">Submit for approval</button>
                            <a class="btn btn-link w-100 mt-2" href="/seller/artifacts">Back to My artifacts</a>
                        </form:form>
                    </div>
                </main>
            </body>

            </html>
