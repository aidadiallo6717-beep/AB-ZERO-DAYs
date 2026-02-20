<?php
/**
 * GHOST-OS - Point d'entrée principal
 * Version: 2.0.0
 */

require_once __DIR__ . '/config/app.php';

// Rediriger vers le panel
header('Location: ' . PANEL_URL);
exit;
