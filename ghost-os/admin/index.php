<?php
/**
 * Interface d'administration
 */

require_once '../config/app.php';

session_start();
if (!isset($_SESSION['admin_id'])) {
    header('Location: login.php');
    exit;
}

$stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
$stmt->execute([$_SESSION['admin_id']]);
$admin = $stmt->fetch();
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GHOST-OS | Administration</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="../assets/css/admin.css">
</head>
<body class="bg-gray-900 text-white">
    
    <div class="flex h-screen">
        <!-- Sidebar -->
        <div class="w-64 bg-gray-800 p-4">
            <h1 class="text-2xl font-bold text-green-400 mb-6">GHOST-OS</h1>
            <nav>
                <a href="index.php" class="block py-2 px-4 bg-gray-700 rounded mb-1">Dashboard</a>
                <a href="users.php" class="block py-2 px-4 hover:bg-gray-700 rounded mb-1">Utilisateurs</a>
                <a href="devices.php" class="block py-2 px-4 hover:bg-gray-700 rounded mb-1">Appareils</a>
                <a href="payments.php" class="block py-2 px-4 hover:bg-gray-700 rounded mb-1">Paiements</a>
                <hr class="border-gray-700 my-4">
                <a href="logout.php" class="block py-2 px-4 hover:bg-red-700 rounded text-red-400">Déconnexion</a>
            </nav>
        </div>
        
        <!-- Main content -->
        <div class="flex-1 p-6 overflow-y-auto">
            <h2 class="text-2xl font-bold mb-4">Dashboard</h2>
            
            <div class="grid grid-cols-4 gap-4 mb-6" id="stats"></div>
            
            <div class="bg-gray-800 p-4 rounded">
                <h3 class="font-bold mb-4">Derniers utilisateurs</h3>
                <div id="users"></div>
            </div>
        </div>
    </div>
    
    <script>
        async function loadStats() {
            let r = await fetch('../api/v1/admin.php?action=stats');
            let data = await r.json();
            document.getElementById('stats').innerHTML = `
                <div class="bg-blue-600 p-4 rounded"><p class="text-blue-200">Utilisateurs</p><p class="text-2xl font-bold">${data.users}</p></div>
                <div class="bg-green-600 p-4 rounded"><p class="text-green-200">Appareils</p><p class="text-2xl font-bold">${data.devices}</p><p class="text-sm">${data.online} en ligne</p></div>
                <div class="bg-purple-600 p-4 rounded"><p class="text-purple-200">Captures</p><p class="text-2xl font-bold">${data.screenshots}</p></div>
                <div class="bg-yellow-600 p-4 rounded"><p class="text-yellow-200">Revenus</p><p class="text-2xl font-bold">${data.revenue} USDT</p></div>
            `;
        }
        
        async function loadUsers() {
            let r = await fetch('../api/v1/admin.php?action=users');
            let users = await r.json();
            let html = '<table class="w-full"><thead><tr class="border-b border-gray-700"><th class="text-left py-2">ID</th><th>Utilisateur</th><th>Email</th><th>Plan</th><th>Appareils</th><th>Inscription</th></tr></thead><tbody>';
            users.forEach(u => {
                html += `<tr class="border-b border-gray-700">
                    <td class="py-2">#${u.id}</td>
                    <td>${u.username}</td>
                    <td>${u.email}</td>
                    <td><span class="px-2 py-1 rounded text-xs ${u.account_type === 'enterprise' ? 'bg-purple-600' : (u.account_type === 'premium' ? 'bg-blue-600' : 'bg-gray-600')}">${u.account_type}</span></td>
                    <td>${u.devices_count}</td>
                    <td>${new Date(u.created_at).toLocaleDateString()}</td>
                </tr>`;
            });
            html += '</tbody></table>';
            document.getElementById('users').innerHTML = html;
        }
        
        loadStats();
        loadUsers();
        setInterval(loadStats, 30000);
    </script>
</body>
</html>
