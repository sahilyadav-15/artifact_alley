<%@ page contentType="text/html;charset=UTF-8" %>
    <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
        <!DOCTYPE html>
        <html lang="en">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Register | Artifact Alley</title>
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
            <link href="/css/site.css" rel="stylesheet">
        </head>

        <body>
            <main class="container py-5">
                <div class="auth-card mx-auto p-4 p-md-5">
                    <h1>Join Artifact Alley</h1>
                    <p class="text-muted" id="registration-helper" aria-live="polite">Find what inspires you. Make it
                        yours.</p>
                    <div class="form-toast" id="password-toast" role="alert" aria-live="assertive" hidden>Enter a
                        password with at least 6 characters.</div>
                    <form:form method="post" modelAttribute="registrationForm" class="mt-4" id="registration-form"
                        novalidate="true">
                        <form:errors path="*" cssClass="alert alert-danger d-block" element="div" />
                        <fieldset class="mb-4 role-fieldset">
                            <legend class="visually-hidden">Choose account type</legend>
                            <div class="role-selector" id="role-selector">
                                <form:radiobutton path="role" value="BIDDER" id="role-bidder"
                                    cssClass="role-option-input" />
                                <label for="role-bidder" class="role-option">
                                    <img src="/images/bidding.png" alt="" class="role-option-icon" />
                                    <span class="role-option-copy"><strong>Bidder</strong><small>Discover &amp;
                                            bid</small></span>
                                </label>
                                <form:radiobutton path="role" value="SELLER" id="role-seller"
                                    cssClass="role-option-input" />
                                <label for="role-seller" class="role-option">
                                    <img src="/images/seller.png" alt="" class="role-option-icon" />
                                    <span class="role-option-copy"><strong>Seller</strong><small>List
                                            artifacts</small></span>
                                </label>
                            </div>
                            <form:errors path="role" cssClass="invalid-feedback d-block" />
                        </fieldset>
                        <div class="mb-3">
                            <form:label path="name" cssClass="form-label">Name</form:label>
                            <form:input path="name" cssClass="form-control" maxlength="60" autocomplete="name" />
                            <form:errors path="name" cssClass="invalid-feedback d-block" />
                        </div>
                        <div class="mb-3">
                            <form:label path="email" cssClass="form-label">Email address</form:label>
                            <form:input path="email" type="email" cssClass="form-control" maxlength="254"
                                autocomplete="email" />
                            <form:errors path="email" cssClass="invalid-feedback d-block" />
                        </div>
                        <div class="mb-3">
                            <form:label path="password" cssClass="form-label">Password</form:label>
                            <div class="password-control">
                                <form:password path="password" id="register-password" cssClass="form-control"
                                    maxlength="72" autocomplete="new-password" /><button class="password-toggle"
                                    type="button" data-password-toggle data-target="register-password"
                                    aria-label="Show password" aria-pressed="false"><span class="password-eye"
                                        aria-hidden="true">◉</span></button>
                            </div>
                            <form:errors path="password" cssClass="invalid-feedback d-block" />
                        </div>
                        <div class="mb-3">
                            <form:label path="confirmPassword" cssClass="form-label">Confirm password</form:label>
                            <div class="password-control">
                                <form:password path="confirmPassword" id="register-confirm-password"
                                    cssClass="form-control" maxlength="72" autocomplete="new-password" /><button
                                    class="password-toggle" type="button" data-password-toggle
                                    data-target="register-confirm-password" aria-label="Show password"
                                    aria-pressed="false"><span class="password-eye" aria-hidden="true">◉</span></button>
                            </div>
                            <form:errors path="confirmPassword" cssClass="invalid-feedback d-block" />
                        </div>
                        <button class="btn btn-primary w-100" type="submit">Create account</button>
                    </form:form>
                    <p class="text-center text-muted mt-4 mb-0">Already registered? <a href="/login">Sign in</a>.</p>
                </div>
            </main>
            <script>
                (() => {
                    const selector = document.getElementById('role-selector');
                    const helper = document.getElementById('registration-helper');
                    const seller = document.getElementById('role-seller');
                    const form = document.getElementById('registration-form');
                    const password = document.getElementById('password');
                    const passwordToast = document.getElementById('password-toast');
                    const updateRoleUi = () => {
                        const isSeller = seller.checked;
                        selector.classList.toggle('seller-selected', isSeller);
                        helper.textContent = isSeller
                            ? 'Turn your artifacts into someone’s next discovery.'
                            : 'Find what inspires you. Make it yours.';
                    };
                    document.querySelectorAll('input[name="role"]').forEach(input => input.addEventListener('change', updateRoleUi));
                    form.addEventListener('submit', event => {
                        if (password.value.length < 6) {
                            event.preventDefault();
                            passwordToast.hidden = false;
                            password.focus();
                        }
                    });
                    password.addEventListener('input', () => { if (password.value.length >= 6) passwordToast.hidden = true; });
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
                    updateRoleUi();
                })();
            </script>
        </body>

        </html>