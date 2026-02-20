<?php
/**
 * API Paiements Crypto
 */

require_once '../../config/app.php';

$method = $_SERVER['REQUEST_METHOD'];
$input = json_decode(file_get_contents('php://input'), true);

// ============================================
// GÉNÉRER UNE ADRESSE DE PAIEMENT
// ============================================
if ($method === 'POST' && ($input['action'] ?? '') === 'generate') {
    
    $plan = $input['plan'] ?? 'premium';
    $currency = $input['currency'] ?? 'USDT';
    
    if (!isset(PLANS[$plan])) sendError('Plan invalide');
    
    $address = '';
    switch ($currency) {
        case 'USDT': $address = USDT_ADDRESS; break;
        case 'BTC': $address = BTC_ADDRESS; break;
        case 'ETH': $address = ETH_ADDRESS; break;
        default: sendError('Devise non supportée');
    }
    
    $tx_ref = 'GHOST_' . bin2hex(random_bytes(16));
    
    $_SESSION['pending_payment'] = [
        'tx_ref' => $tx_ref,
        'plan' => $plan,
        'amount' => PLANS[$plan]['price'],
        'currency' => $currency,
        'address' => $address,
        'created_at' => time()
    ];
    
    sendJson([
        'success' => true,
        'address' => $address,
        'amount' => PLANS[$plan]['price'],
        'currency' => $currency,
        'tx_ref' => $tx_ref,
        'qr_code' => "https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=$address"
    ]);
}

// ============================================
// VÉRIFIER UN PAIEMENT
// ============================================
if ($method === 'POST' && ($input['action'] ?? '') === 'verify') {
    
    $tx_ref = $input['tx_ref'] ?? '';
    $tx_hash = $input['tx_hash'] ?? '';
    
    $pending = $_SESSION['pending_payment'] ?? null;
    if (!$pending || $pending['tx_ref'] !== $tx_ref) {
        sendError('Transaction non trouvée');
    }
    
    // Simulation de vérification (à remplacer par API blockchain)
    if (strlen($tx_hash) > 10) {
        
        $headers = getallheaders();
        $api_key = $headers['X-API-Key'] ?? '';
        
        if ($api_key) {
            $stmt = $pdo->prepare("SELECT * FROM users WHERE api_key = ?");
            $stmt->execute([$api_key]);
            $user = $stmt->fetch();
            
            if ($user) {
                $plan = $pending['plan'];
                $days = PLANS[$plan]['days'];
                
                $pdo->beginTransaction();
                
                $pdo->prepare("
                    INSERT INTO transactions (user_id, tx_hash, amount, currency, plan, status)
                    VALUES (?, ?, ?, ?, ?, 'confirmed')
                ")->execute([$user['id'], $tx_hash, $pending['amount'], $pending['currency'], $plan]);
                
                $pdo->prepare("
                    UPDATE users
                    SET account_type = ?,
                        subscription_start = NOW(),
                        subscription_end = DATE_ADD(NOW(), INTERVAL ? DAY)
                    WHERE id = ?
                ")->execute([$plan, $days, $user['id']]);
                
                $pdo->commit();
                
                unset($_SESSION['pending_payment']);
                logActivity($pdo, $user['id'], 'payment_success', $plan);
                
                sendJson(['success' => true]);
                exit;
            }
        }
    }
    
    sendJson(['success' => false, 'message' => 'Transaction non confirmée']);
}

sendError('Action non valide', 400);
?>
