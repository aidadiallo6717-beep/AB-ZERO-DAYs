<?php
/**
 * Inscription
 */

require_once '../config/app.php';
?>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GHOST-OS | Inscription</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-900">
    <div class="min-h-screen flex items-center justify-center">
        <div class="bg-gray-800 p-8 rounded-lg w-96">
            <h1 class="text-3xl font-bold text-green-400 text-center mb-6">GHOST-OS</h1>
            <p class="text-gray-400 text-center mb-6">Inscription</p>
            
            <form id="registerForm">
                <div class="mb-4">
                    <input type="text" id="username" placeholder="Nom d'utilisateur" 
                           class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                </div>
                <div class="mb-4">
                    <input type="email" id="email" placeholder="Email" 
                           class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                </div>
                <div class="mb-4">
                    <input type="password" id="password" placeholder="Mot de passe (min 8 caractères)" 
                           class="w-full bg-gray-700 border border-gray-600 rounded px-4 py-2 text-white" required>
                </div>
                <button type="submit" 
                        class="w-full bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded mb-4">
                    S'inscrire
                </button>
            </form>
            
            <p class="text-center text-gray-400">
                Déjà inscrit ? <a href="index.php" class="text-green-400">Se connecter</a>
            </p>
        </div>
    </div>
    
    <script>
        document.getElementById('registerForm').onsubmit = async (e) => {
            e.preventDefault();
            
            if (document.getElementById('password').value.length < 8) {
                alert('Le mot de passe doit contenir au moins 8 caractères');
                return;
            }
            
            let r = await fetch('../api/v1/auth.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: 'register',
                    username: document.getElementById('username').value,
                    email: document.getElementById('email').value,
                    password: document.getElementById('password').value
                })
            });
            let data = await r.json();
            if (data.success) {
                alert('Inscription réussie ! Vous pouvez maintenant vous connecter.');
                window.location.href = 'index.php';
            } else {
                alert(data.error || 'Erreur lors de l\'inscription');
            }
        };
    </script>
</body>
</html>
