<?php
/**
 * Script d'installation
 */

echo "GHOST-OS Installation\n";
echo "====================\n\n";

// Vérifier PHP
echo "Checking PHP... " . (version_compare(PHP_VERSION, '7.4.0', '>=') ? "OK" : "FAILED") . "\n";

// Vérifier extensions
$extensions = ['pdo_mysql', 'json', 'gd', 'curl', 'openssl'];
foreach ($extensions as $ext) {
    echo "Checking $ext... " . (extension_loaded($ext) ? "OK" : "FAILED") . "\n";
}

// Créer les dossiers
$dirs = ['uploads', 'uploads/screens', 'uploads/camera', 'uploads/audio', 'uploads/files', 'logs'];
foreach ($dirs as $dir) {
    if (!is_dir("../$dir")) mkdir("../$dir", 0755, true);
    echo "Creating $dir... OK\n";
}

// Demander config DB
echo "\nDatabase Configuration:\n";
$db_host = readline("Host [localhost]: ") ?: 'localhost';
$db_name = readline("Database name [ghost_os]: ") ?: 'ghost_os';
$db_user = readline("Username: ");
$db_pass = readline("Password: ");

// Tester connexion
try {
    $pdo = new PDO("mysql:host=$db_host", $db_user, $db_pass);
    echo "Database connection: OK\n";
    
    // Créer base
    $pdo->exec("CREATE DATABASE IF NOT EXISTS $db_name");
    $pdo->exec("USE $db_name");
    
    // Importer SQL
    $sql = file_get_contents('database.sql');
    $pdo->exec($sql);
    echo "Database created: OK\n";
    
} catch (PDOException $e) {
    die("Database error: " . $e->getMessage() . "\n");
}

// Créer fichier de config
$config = "<?php
define('DB_HOST', '$db_host');
define('DB_NAME', '$db_name');
define('DB_USER', '$db_user');
define('DB_PASS', '$db_pass');
define('DB_CHARSET', 'utf8mb4');
?>";

file_put_contents('../config/database.php', $config);
echo "Config file created: OK\n";

// Final
echo "\n✅ Installation complete!\n";
echo "Panel: https://your-domain.com/panel\n";
echo "Admin: https://your-domain.com/admin (admin@ghost-os.com / Admin123!)\n";
echo "\nDon't forget to:\n";
echo "1. Change default admin password\n";
echo "2. Configure your crypto addresses in config/constants.php\n";
echo "3. Start WebSocket server: cd websocket && npm install && npm start\n";
