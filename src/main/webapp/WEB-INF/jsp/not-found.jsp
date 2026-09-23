<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Artifact not found | Artifact Alley</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/site.css" rel="stylesheet">
</head>
<body>
<main class="container py-5">
    <section class="auth-card mx-auto p-4 p-md-5" aria-labelledby="not-found-title">
        <p class="eyebrow text-dark">404</p>
        <h1 id="not-found-title">Artifact not found</h1>
        <p class="description"><c:out value="${errorMessage}" /></p>
        <a class="btn btn-primary" href="/">Return to live auctions</a>
    </section>
</main>
</body>
</html>
