<?php
setcookie('api_key', '', time() - 3600, '/');
header('Location: index.php');
exit;
