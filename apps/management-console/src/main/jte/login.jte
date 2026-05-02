@param String error = ""
@param String logout = ""
@param org.springframework.security.web.csrf.CsrfToken _csrf

<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login — The Amateur Naturalist</title>
    <link rel="stylesheet" href="/css/pico.min.css">
    <link rel="stylesheet" href="/css/naturalist.css">
    <style>
        .login-container {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 1rem;
        }
        .login-card {
            background: var(--content-background);
            padding: 2rem;
            border-radius: 0.5rem;
            max-width: 400px;
            width: 100%;
        }
        .login-card h1 {
            margin-top: 0;
            text-align: center;
        }
        .login-form {
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }
        .login-form input {
            padding: 0.75rem;
            border: 1px solid var(--pico-card-border-color);
            border-radius: 0.25rem;
        }
        .login-form button {
            padding: 0.75rem;
            background: var(--pico-primary);
            color: white;
            border: none;
            border-radius: 0.25rem;
            cursor: pointer;
            font-weight: 600;
        }
        .login-form button:hover {
            background: var(--pico-primary-hover);
        }
        .error {
            background: rgba(200, 100, 100, 0.8);
            color: #FAF8F5;
            padding: 0.75rem;
            border-radius: 0.25rem;
            margin-bottom: 1rem;
        }
        .logout {
            background: rgba(100, 150, 100, 0.8);
            color: #FAF8F5;
            padding: 0.75rem;
            border-radius: 0.25rem;
            margin-bottom: 1rem;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="login-card">
            <h1>The Amateur Naturalist</h1>

            @if(!error.isEmpty())
                <div class="error">Login failed. Please check your username and password.</div>
            @endif

            @if(!logout.isEmpty())
                <div class="logout">You have been logged out.</div>
            @endif

            <form method="post" action="/login" class="login-form">
                @if(_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <div>
                    <label for="username">Username</label>
                    <input type="text" id="username" name="username" required autofocus>
                </div>
                <div>
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <button type="submit">Login</button>
            </form>
        </div>
    </div>

    <div class="background-opacity-control">
        <div class="opacity-label">Image Opacity</div>
        <input type="range" id="bg-opacity-slider" min="0" max="100" value="100" class="opacity-slider">
        <div id="opacity-display">100%</div>
    </div>
    <script>
        const slider = document.getElementById('bg-opacity-slider');
        const display = document.getElementById('opacity-display');

        function updateOpacity() {
            const value = slider.value;
            const imageOpacity = value / 100;
            document.documentElement.style.setProperty('--image-opacity', imageOpacity);
            display.textContent = value + '%';
        }

        slider.addEventListener('input', updateOpacity);
        updateOpacity();
    </script>
</body>
</html>
