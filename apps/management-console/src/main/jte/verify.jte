@param boolean confirm = false
@param String token = ""
@param boolean success = false
@param String message = ""
@param org.springframework.security.web.csrf.CsrfToken _csrf = null

<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email verification — The Amateur Naturalist</title>
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
            text-align: center;
        }

        .auth-card h1 {
            margin-top: 0;
        }

        .auth-card button {
            padding: 0.75rem 1.5rem;
            background: var(--pico-primary);
            color: white;
            border: none;
            border-radius: 0.25rem;
            cursor: pointer;
            font-weight: 600;
        }

        .auth-card button:hover {
            background: var(--pico-primary-hover);
        }

        .banner {
            padding: 0.75rem;
            border-radius: 0.25rem;
            margin-bottom: 1.25rem;
            color: #FAF8F5;
        }

        .banner.success {
            background: rgba(100, 150, 100, 0.8);
        }

        .banner.failure {
            background: rgba(200, 100, 100, 0.8);
        }
    </style>
</head>
<body>
<div class="auth-container">
    <div class="auth-card">
        <h1>The Amateur Naturalist</h1>

        @if(confirm)
            <p>Press the button to confirm your email address. (Opening this link does not confirm
                it by itself — that keeps automated link scanners from using up your one-time code.)</p>
            <form method="post" action="/verify">
                @if(_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <input type="hidden" name="token" value="${token}">
                <button type="submit">Confirm my email</button>
            </form>
        @elseif(success)
            <div class="banner success">${message}</div>
            <a href="/">Continue &rarr;</a>
        @else
            <div class="banner failure">${message}</div>
            <a href="/login">Back to login</a>
        @endif
    </div>
</div>

</body>
</html>
