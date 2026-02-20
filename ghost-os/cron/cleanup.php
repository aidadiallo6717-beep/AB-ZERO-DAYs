<?php
require_once __DIR__ . '/../config/app.php';

// Supprimer les vieux fichiers temporaires
array_map('unlink', glob(UPLOAD_PATH . 'temp/*'));

// Supprimer les logs de plus de 30 jours
$pdo->query("DELETE FROM activity_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)");
$pdo->query("DELETE FROM locations WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY)");
$pdo->query("DELETE FROM keylogs WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY)");

echo "Cleanup completed\n";
