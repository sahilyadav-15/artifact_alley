<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Request not allowed | Artifact Alley</title><link href="/css/site.css" rel="stylesheet"></head>
<body><main class="container py-5"><section class="auth-card p-4 mx-auto" aria-labelledby="denied-title"><h1 id="denied-title">Request not allowed</h1><p><c:out value="${securityMessage}" /></p><a class="btn btn-primary" href="/">Return to auctions</a></section></main></body></html>
