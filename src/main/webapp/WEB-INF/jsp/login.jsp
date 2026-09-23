<%@ page contentType="text/html;charset=UTF-8" %>
    <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
        <%@ taglib prefix="c" uri="jakarta.tags.core" %>
            <!DOCTYPE html>
            <html lang="en">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sign in | Artifact Alley</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <link href="/css/site.css" rel="stylesheet">
            </head>

            <body>
                <main class="container py-5">
                    <div class="auth-card mx-auto p-4 p-md-5">
                        <p class="eyebrow text-dark">WELCOME BACK</p>
                        <h1>Sign in</h1>
                        <c:if test="${not empty errorMessage}">
                            <div class="alert alert-danger" role="alert">${errorMessage}</div>
                        </c:if>
                        <form:form method="post" modelAttribute="loginForm" class="mt-4" novalidate="true">
                            <form:errors path="*" cssClass="alert alert-danger d-block" element="div" />
                            <div class="mb-3">
                                <form:label path="email" cssClass="form-label">Email address</form:label>
                                <form:input path="email" type="email" cssClass="form-control" maxlength="254"
                                    autocomplete="email" />
                                <form:errors path="email" cssClass="invalid-feedback d-block" />
                            </div>
                            <div class="mb-4">
                                <form:label path="password" cssClass="form-label">Password</form:label>
                                <div class="password-control">
                                    <form:password path="password" id="login-password" cssClass="form-control"
                                        maxlength="72" autocomplete="current-password" /><button class="password-toggle"
                                        type="button" data-password-toggle data-target="login-password"
                                        aria-label="Show password" aria-pressed="false"><svg class="eye-open"
                                            viewBox="0 0 24 24" aria-hidden="true">
                                            <path
                                                d="M12 5C6.4 5 2.3 9.1 1 12c1.3 2.9 5.4 7 11 7s9.7-4.1 11-7c-1.3-2.9-5.4-7-11-7Zm0 11.5A4.5 4.5 0 1 1 12 7a4.5 4.5 0 0 1 0 9.5Zm0-2A2.5 2.5 0 1 0 12 9a2.5 2.5 0 0 0 0 5.5Z" />
                                        </svg><svg class="eye-closed" viewBox="0 0 24 24" aria-hidden="true">
                                            <path
                                                d="m3 4.3 1.4-1.4L20.7 19.2l-1.4 1.4-2.5-2.5c-1.5.6-3.1.9-4.8.9-5.6 0-9.7-4.1-11-7  .5-1.1 1.4-2.5 2.7-3.7L5.1 9.7C4.2 10.5 3.5 11.4 3 12c1.2 2.4 4.5 5 9 5 1.1 0 2.1-.2 3.1-.5l-1.8-1.8a4.5 4.5 0 0 1-4-4l-2-2C6.6 9.2 6 9.8 5.5 10.5L4.1 9.1A14.2 14.2 0 0 0 1 12c1.3 2.9 5.4 7 11 7 2.3 0 4.4-.7 6.1-1.7l2.6 2.6 1.4-1.4L3 4.3ZM12 7c.7 0 1.4.2 2 .5l-1.7 1.7A2.5 2.5 0 0 0 9.2 12L7.5 10.3A4.5 4.5 0 0 1 12 7Z" />
                                        </svg></button>
                                </div>
                                <form:errors path="password" cssClass="invalid-feedback d-block" />
                            </div>
                            <button class="btn btn-primary w-100" type="submit">Sign in</button>
                        </form:form>
                        <p class="text-center text-muted mt-4 mb-0">New here? <a href="/register">Create an account</a>.
                        </p>
                    </div>
                </main>
                <script>
                    document.querySelectorAll('[data-password-toggle]').forEach(button => {
                        button.addEventListener('click', () => {
                            const input = document.getElementById(button.dataset.target);
                            const visible = input.type === 'text';
                            input.type = visible ? 'password' : 'text';
                            button.setAttribute('aria-pressed', String(!visible));
                            button.setAttribute('aria-label', visible ? 'Show password' : 'Hide password');
                            button.classList.toggle('is-visible', !visible);
                        });
                    });
                </script>
            </body>

            </html>