<?php
/**
 * API Commandes
 */

require_once '../../config/app.php';
$user = checkAuth($pdo);

$device_id = $_GET['device_id'] ?? 0;
if (!$device_id) sendError('device_id requis');

// Vérifier que l'appareil appartient à l'utilisateur
$stmt = $pdo->prepare("SELECT id FROM devices WHERE id = ? AND user_id = ?");
$stmt->execute([$device_id, $user['id']]);
if (!$stmt->fetch()) {
    sendError('Appareil non trouvé', 403);
}

$method = $_SERVER['REQUEST_METHOD'];

// ============================================
// GET - Récupérer les commandes en attente
// ============================================
if ($method === 'GET') {
    $stmt = $pdo->prepare("
        SELECT * FROM commands
        WHERE device_id = ? AND status = 'pending'
        ORDER BY created_at ASC
        LIMIT 50
    ");
    $stmt->execute([$device_id]);
    $commands = $stmt->fetchAll();
    
    // Marquer comme envoyées
    if (!empty($commands)) {
        $ids = array_column($commands, 'id');
        $pdo->prepare("UPDATE commands SET status = 'sent' WHERE id IN (" . implode(',', $ids) . ")")
            ->execute();
    }
    
    sendJson(['commands' => $commands]);
}

// ============================================
// POST - Ajouter une commande
// ============================================
if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    $command = $input['command'] ?? '';
    $params = $input['params'] ?? '';
    
    $stmt = $pdo->prepare("
        INSERT INTO commands (device_id, command, params, status)
        VALUES (?, ?, ?, 'pending')
    ");
    
    if ($stmt->execute([$device_id, $command, $params])) {
        logActivity($pdo, $user['id'], 'command_sent', $command);
        sendJson(['success' => true, 'command_id' => $pdo->lastInsertId()]);
    } else {
        sendError('Erreur lors de l\'ajout', 500);
    }
}

sendError('Méthode non autorisée', 405);
?>
