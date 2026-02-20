<?php
/**
 * Login administrateur
 */

require_once '../config/app.php';

session_start();
if (isset($_SESSION['admin_id'])) {
    header('Location: index.php');
    exit;
}

$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = $_POST['username'] ?? '';
    $password = $_POST['password'] ?? '';
    
    $stmt = $pdo->prepare("SELECT * FROM users WHERE username = ? AND account_type = 'admin'");
    $stmt->execute([$username]);
    $admin = $stmt->fetch();
    
    if ($admin && password_verify($password, $admin['password_hash'])) {
        $_SESSION['admin_id'] = $admin['id'];
        $_SESSION['admin_username'] = $admin['username'];
        header('Location: index.php');
        exit;
    } else {
        $error = 'Identifiants incorrects';
    }
}
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GHOST-OS | Admin Login</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900">
    <div class="min-h-screen flex items-center justify-center">
        <div class="bg-gray-800 p-8 rounded-lg w-96">
            <h1 class="text-3xl font-bold text-green-400 text-center mb-6">GHOST-OS</h1>
            <p class="text-gray-400 text-center mb-6">Administration</p>
            
            <?php if ($error): ?>
                <div class="bg-red-500 text-white p-3 rounded mb-4"><?= $error ?></div>
            <?php endif; ?>
            
            <form method="POST">
                <div class="mb-4">
                    <input type="text" name="username" placeholder="Nom d'utilisateur" 
                           class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                </div>
                <div class="mb-4">
                    <input type="password" name="password" placeholder="Mot de passe" 
                           class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                </div>
                <button type="submit" 
                        class="w-full bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded">
                    Se connecter
                </button>
            </form>
        </div>
    </div>
</body>
</html>
