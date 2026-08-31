@param boolean success = false
@param String message = ""

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

        @if(success)
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
