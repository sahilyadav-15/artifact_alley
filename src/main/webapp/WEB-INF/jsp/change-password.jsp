<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Change password | Artifact Alley</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link href="/css/site.css" rel="stylesheet">
</head><body>
<nav class="navbar navbar-dark auction-nav"><div class="container"><a class="navbar-brand" href="/">Artifact Alley</a><span class="navbar-text">Account settings</span></div></nav>
<main class="container py-5"><div class="auth-card mx-auto p-4 p-md-5">
    <p class="eyebrow text-dark">ACCOUNT SECURITY</p><h1>Change password</h1>
    <form:form method="post" modelAttribute="changePasswordForm" class="mt-4" novalidate="true">
        <form:errors path="*" cssClass="alert alert-danger d-block" element="div"/>
        <div class="mb-3"><form:label path="currentPassword" cssClass="form-label">Current password</form:label><form:password path="currentPassword" cssClass="form-control" maxlength="72" autocomplete="current-password"/><form:errors path="currentPassword" cssClass="invalid-feedback d-block"/></div>
        <div class="mb-3"><form:label path="newPassword" cssClass="form-label">New password</form:label><form:password path="newPassword" cssClass="form-control" maxlength="72" autocomplete="new-password"/><form:errors path="newPassword" cssClass="invalid-feedback d-block"/></div>
        <div class="mb-4"><form:label path="confirmPassword" cssClass="form-label">Confirm new password</form:label><form:password path="confirmPassword" cssClass="form-control" maxlength="72" autocomplete="new-password"/><form:errors path="confirmPassword" cssClass="invalid-feedback d-block"/></div>
        <button class="btn btn-primary w-100" type="submit">Save new password</button>
    </form:form>
</div></main>
</body></html>
