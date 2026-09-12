<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign in | Artifact Alley</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="/css/site.css" rel="stylesheet">
</head><body>
<main class="container py-5"><div class="auth-card mx-auto p-4 p-md-5">
    <p class="eyebrow text-dark">WELCOME BACK</p><h1>Sign in</h1>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert">${errorMessage}</div></c:if>
    <form:form method="post" modelAttribute="loginForm" class="mt-4" novalidate="true">
        <form:errors path="*" cssClass="alert alert-danger d-block" element="div" />
        <div class="mb-3"><form:label path="email" cssClass="form-label">Email address</form:label><form:input path="email" type="email" cssClass="form-control" maxlength="254" autocomplete="email"/><form:errors path="email" cssClass="invalid-feedback d-block"/></div>
        <div class="mb-4"><form:label path="password" cssClass="form-label">Password</form:label><form:password path="password" cssClass="form-control" maxlength="72" autocomplete="current-password"/><form:errors path="password" cssClass="invalid-feedback d-block"/></div>
        <button class="btn btn-primary w-100" type="submit">Sign in</button>
    </form:form>
    <p class="text-center text-muted mt-4 mb-0">New here? <a href="/register">Create an account</a>.</p>
</div></main>
</body></html>
