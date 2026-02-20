<?php
/**
 * Panel utilisateur
 */

require_once '../config/app.php';

$user = null;
$api_key = $_COOKIE['api_key'] ?? '';

if ($api_key) {
    $stmt = $pdo->prepare("SELECT * FROM users WHERE api_key = ?");
    $stmt->execute([$api_key]);
    $user = $stmt->fetch();
}
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GHOST-OS | Panel</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900 text-white">
    
    <?php if ($user): ?>
        <div class="container mx-auto px-4 py-8">
            <div class="flex justify-between items-center mb-8">
                <h1 class="text-3xl font-bold text-green-400">GHOST-OS</h1>
                <div class="flex items-center">
                    <span class="mr-4"><?= htmlspecialchars($user['username']) ?></span>
                    <a href="logout.php" class="bg-red-600 px-4 py-2 rounded">Déconnexion</a>
                </div>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div class="bg-gray-800 p-6 rounded-lg">
                    <h2 class="text-xl font-bold mb-4">📱 Appareils</h2>
                    <div id="devices">Chargement...</div>
                </div>
                
                <div class="bg-gray-800 p-6 rounded-lg">
                    <h2 class="text-xl font-bold mb-4">💳 Abonnement</h2>
                    <p class="text-2xl font-bold text-green-400"><?= $user['account_type'] ?></p>
                    <?php if ($user['subscription_end']): ?>
                        <p class="text-sm text-gray-400">Expire le <?= date('d/m/Y', strtotime($user['subscription_end'])) ?></p>
                    <?php elseif ($user['trial_end']): ?>
                        <p class="text-sm text-gray-400">Essai jusqu'au <?= date('d/m/Y', strtotime($user['trial_end'])) ?></p>
                    <?php endif; ?>
                </div>
            </div>
        </div>
        
        <script>
            async function loadDevices() {
                let r = await fetch('../api/v1/devices.php', {
                    headers: { 'X-API-Key': '<?= $user['api_key'] ?>' }
                });
                let data = await r.json();
                let html = '';
                data.devices.forEach(d => {
                    html += `<div class="bg-gray-700 p-3 rounded mb-2">
                        <p class="font-bold">${d.device_name}</p>
                        <p class="text-sm text-gray-400">${d.model} | ${d.android_version}</p>
                        <p class="text-xs ${d.online ? 'text-green-400' : 'text-gray-500'}">${d.online ? 'En ligne' : 'Hors ligne'}</p>
                    </div>`;
                });
                document.getElementById('devices').innerHTML = html || '<p class="text-gray-400">Aucun appareil</p>';
            }
            loadDevices();
        </script>
        
    <?php else: ?>
        <div class="min-h-screen flex items-center justify-center">
            <div class="bg-gray-800 p-8 rounded-lg w-96">
                <h1 class="text-3xl font-bold text-green-400 text-center mb-6">GHOST-OS</h1>
                
                <form id="loginForm">
                    <div class="mb-4">
                        <input type="email" id="email" placeholder="Email" 
                               class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                    </div>
                    <div class="mb-4">
                        <input type="password" id="password" placeholder="Mot de passe" 
                               class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                    </div>
                    <button type="submit" 
                            class="w-full bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded mb-4">
                        Se connecter
                    </button>
                </form>
                
                <p class="text-center text-gray-400">
                    Pas encore de compte ? <a href="register.php" class="text-green-400">S'inscrire</a>
                </p>
            </div>
        </div>
        
        <script>
            document.getElementById('loginForm').onsubmit = async (e) => {
                e.preventDefault();
                let r = await fetch('../api/v1/auth.php', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        action: 'login',
                        email: document.getElementById('email').value,
                        password: document.getElementById('password').value
                    })
                });
                let data = await r.json();
                if (data.success) {
                    document.cookie = `api_key=${data.api_key}; path=/; max-age=86400`;
                    window.location.reload();
                } else {
                    alert(data.error || 'Erreur de connexion');
                }
            };
        </script>
    <?php endif; ?>
    
</body>
</html>
