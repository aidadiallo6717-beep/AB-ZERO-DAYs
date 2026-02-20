<?php
/**
 * Configuration principale
 */

error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', LOG_PATH . '/error.log');

date_default_timezone_set('UTC');

session_name('GHOST_SESSION');
session_start();

require_once __DIR__ . '/database.php';
require_once __DIR__ . '/constants.php';
require_once __DIR__ . '/../includes/functions.php';
?>
