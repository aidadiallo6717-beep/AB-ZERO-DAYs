<?php
/**
 * API Administration
 */

require_once '../../config/app.php';

session_start();
if (!isset($_SESSION['admin_id'])) {
    sendError('Non autorisé', 401);
}

$action = $_GET['action'] ?? '';

// ============================================
// STATISTIQUES
// ============================================
if ($action === 'stats') {
    $stats = [
        'users' => $pdo->query("SELECT COUNT(*) FROM users")->fetchColumn(),
        'devices' => $pdo->query("SELECT COUNT(*) FROM devices")->fetchColumn(),
        'online' => $pdo->query("SELECT COUNT(*) FROM devices WHERE last_seen > DATE_SUB(NOW(), INTERVAL 5 MINUTE)")->fetchColumn(),
        'screenshots' => $pdo->query("SELECT COUNT(*) FROM screenshots")->fetchColumn(),
        'locations' => $pdo->query("SELECT COUNT(*) FROM locations")->fetchColumn(),
        'revenue' => $pdo->query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE status = 'confirmed'")->fetchColumn(),
        'premium' => $pdo->query("SELECT COUNT(*) FROM users WHERE account_type = 'premium'")->fetchColumn(),
        'enterprise' => $pdo->query("SELECT COUNT(*) FROM users WHERE account_type = 'enterprise'")->fetchColumn()
    ];
    sendJson($stats);
}

// ============================================
// LISTE DES UTILISATEURS
// ============================================
if ($action === 'users') {
    $stmt = $pdo->query("
        SELECT u.*, COUNT(d.id) as devices_count
        FROM users u
        LEFT JOIN devices d ON d.user_id = u.id
        GROUP BY u.id
        ORDER BY u.created_at DESC
        LIMIT 100
    ");
    sendJson($stmt->fetchAll());
}

// ============================================
// BANIR UN UTILISATEUR
// ============================================
if ($action === 'ban' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    $user_id = $input['user_id'] ?? 0;
    $reason = $input['reason'] ?? '';
    
    $pdo->prepare("UPDATE users SET account_status = 'banned' WHERE id = ?")
        ->execute([$user_id]);
    
    $pdo->prepare("INSERT INTO bans (user_id, reason, banned_by) VALUES (?, ?, ?)")
        ->execute([$user_id, $reason, $_SESSION['admin_id']]);
    
    sendJson(['success' => true]);
}

sendError('Action non valide');
?>
