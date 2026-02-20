<?php
/**
 * Fonctions utilitaires
 */

function sanitize($input) {
    return htmlspecialchars(trim($input), ENT_QUOTES, 'UTF-8');
}

function validateEmail($email) {
    return filter_var($email, FILTER_VALIDATE_EMAIL) !== false;
}

function validatePhone($phone) {
    return preg_match('/^[0-9+\-\s\(\)]{8,20}$/', $phone);
}

function timeAgo($timestamp) {
    $time = strtotime($timestamp);
    $diff = time() - $time;
    
    if ($diff < 60) return 'à l\'instant';
    if ($diff < 3600) return floor($diff / 60) . ' min';
    if ($diff < 86400) return floor($diff / 3600) . ' h';
    if ($diff < 2592000) return floor($diff / 86400) . ' j';
    return date('d/m/Y', $time);
}

function formatBytes($bytes, $precision = 2) {
    $units = ['B', 'KB', 'MB', 'GB', 'TB'];
    $bytes = max($bytes, 0);
    $pow = floor(($bytes ? log($bytes) : 0) / log(1024));
    $pow = min($pow, count($units) - 1);
    return round($bytes / pow(1024, $pow), $precision) . ' ' . $units[$pow];
}

function sendJson($data, $code = 200) {
    http_response_code($code);
    header('Content-Type: application/json');
    echo json_encode($data);
    exit;
}

function sendError($message, $code = 400) {
    sendJson(['error' => $message], $code);
}

function checkAuth($pdo) {
    $headers = getallheaders();
    $api_key = $headers['X-API-Key'] ?? '';
    
    if (!$api_key) {
        sendError('API key requise', 401);
    }
    
    $stmt = $pdo->prepare("SELECT * FROM users WHERE api_key = ?");
    $stmt->execute([$api_key]);
    $user = $stmt->fetch();
    
    if (!$user) {
        sendError('API key invalide', 403);
    }
    
    return $user;
}
?>
