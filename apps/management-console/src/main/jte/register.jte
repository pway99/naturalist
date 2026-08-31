@param boolean registered = false
@param String error = ""
@param String email = ""
@param String publicHandle = ""
@param String givenName = ""
@param String familyName = ""
@param org.springframework.security.web.csrf.CsrfToken _csrf = null

<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register — The Amateur Naturalist</title>
    <link rel="stylesheet" href="/css/pico.min.css">
    <link rel="stylesheet" href="/css/naturalist.css">
    <style>
        .auth-container {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 1rem;
        }

        .auth-card {
            background: var(--parchment);
            border: 1px solid var(--sepia-rule);
            box-shadow: var(--plate-shadow);
            padding: 2rem;
            border-radius: 0.5rem;
            max-width: 440px;
            width: 100%;
        }

        .auth-card h1 {
            margin-top: 0;
            text-align: center;
        }

        .auth-form {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }

        .auth-form input {
            padding: 0.75rem;
            border: 1px solid var(--pico-card-border-color);
            border-radius: 0.25rem;
        }

        .auth-form button {
            padding: 0.75rem;
            background: var(--pico-primary);
            color: white;
            border: none;
            border-radius: 0.25rem;
            cursor: pointer;
            font-weight: 600;
        }

        .auth-form button:hover {
            background: var(--pico-primary-hover);
        }

        .auth-form small {
            color: var(--pico-muted-color);
        }

        .error {
            background: rgba(200, 100, 100, 0.8);
            color: #FAF8F5;
            padding: 0.75rem;
            border-radius: 0.25rem;
            margin-bottom: 1rem;
        }

        .success {
            background: rgba(100, 150, 100, 0.8);
            color: #FAF8F5;
            padding: 0.75rem;
            border-radius: 0.25rem;
            margin-bottom: 1rem;
        }

        .auth-links {
            margin-top: 1.25rem;
            text-align: center;
        }
    </style>
</head>
<body>
<div class="auth-container">
    <div class="auth-card">
        <h1>The Amateur Naturalist</h1>

        @if(registered)
            <div class="success">
                You're in — welcome! Check your email for a link to confirm your address and
                unlock vision identification. (In development the link is printed to the
                application log.)
            </div>
            <div class="auth-links">
                <a href="/">Start exploring &rarr;</a>
            </div>
        @else
            @if(!error.isEmpty())
                <div class="error">${error}</div>
            @endif

            <form method="post" action="/register" class="auth-form">
                @if(_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <div>
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" value="${email}" required autofocus>
                </div>
                <div>
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" minlength="8" required>
                    <small>At least 8 characters.</small>
                </div>
                <div>
                    <label for="publicHandle">Public handle</label>
                    <input type="text" id="publicHandle" name="publicHandle" value="${publicHandle}" required>
                    <small>How you'll be known in the field guide. You can change it later.</small>
                </div>
                <div>
                    <label for="givenName">First name</label>
                    <input type="text" id="givenName" name="givenName" value="${givenName}" required>
                </div>
                <div>
                    <label for="familyName">Last name <small>(optional)</small></label>
                    <input type="text" id="familyName" name="familyName" value="${familyName}">
                </div>
                <button type="submit">Create account</button>
            </form>
            <div class="auth-links">
                <a href="/login">Already have an account? Log in</a>
            </div>
        @endif
    </div>
</div>

</body>
</html>
