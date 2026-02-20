<?php
/**
 * API Gestion des appareils
 */

require_once '../../config/app.php';
$user = checkAuth($pdo);

$method = $_SERVER['REQUEST_METHOD'];

// ============================================
// GET - Liste des appareils
// ============================================
if ($method === 'GET') {
    $stmt = $pdo->prepare("
        SELECT d.*, 
               (SELECT COUNT(*) FROM screenshots WHERE device_id = d.id) as screenshots,
               (SELECT COUNT(*) FROM locations WHERE device_id = d.id) as locations
        FROM devices d
        WHERE d.user_id = ?
        ORDER BY d.last_seen DESC
    ");
    $stmt->execute([$user['id']]);
    $devices = $stmt->fetchAll();
    
    foreach ($devices as &$d) {
        $d['online'] = $d['last_seen'] && (time() - strtotime($d['last_seen'])) < 300;
        $d['last_seen_ago'] = $d['last_seen'] ? timeAgo($d['last_seen']) : 'Jamais';
    }
    
    sendJson(['devices' => $devices]);
}

// ============================================
// POST - Enregistrer un appareil
// ============================================
if ($method === 'POST') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Vérifier la limite
    $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM devices WHERE user_id = ?");
    $stmt->execute([$user['id']]);
    $count = $stmt->fetch()['count'];
    
    $max = PLANS[$user['account_type']]['max_devices'] ?? 1;
    if ($count >= $max) {
        sendError('Limite d\'appareils atteinte', 403);
    }
    
    $device_id = $input['device_id'] ?? '';
    $device_name = $input['device_name'] ?? 'Android Device';
    $model = $input['model'] ?? '';
    $android_version = $input['android_version'] ?? '';
    
    $stmt = $pdo->prepare("
        INSERT INTO devices (user_id, device_id, device_name, model, android_version, last_seen)
        VALUES (?, ?, ?, ?, ?, NOW())
        ON DUPLICATE KEY UPDATE
            device_name = VALUES(device_name),
            model = VALUES(model),
            android_version = VALUES(android_version),
            last_seen = NOW()
    ");
    
    if ($stmt->execute([$user['id'], $device_id, $device_name, $model, $android_version])) {
        logActivity($pdo, $user['id'], 'device_registered', $device_id);
        sendJson(['success' => true]);
    } else {
        sendError('Erreur lors de l\'enregistrement', 500);
    }
}

sendError('Méthode non autorisée', 405);
?>
