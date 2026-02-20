# Mettre à jour
apt update && apt upgrade -y

# Installer dépendances
apt install -y nginx mysql-server php8.1-fpm php8.1-mysql php8.1-curl php8.1-gd php8.1-zip php8.1-mbstring nodejs npm git

# Créer dossier projet
mkdir -p /var/www/ghost-os
cd /var/www/ghost-os

# Copier tous les fichiers du serveur ici
# (utilisez scp ou ftp)

# Installer dépendances PHP
composer install

# Installer WebSocket
cd websocket
npm install
npm install -g pm2
pm2 start server.js --name ghost-ws
pm2 save
pm2 startup

# Configurer base de données
mysql -u root -p < install/database.sql

# Configurer Nginx
cat > /etc/nginx/sites-available/ghost-os << EOF
server {
    listen 80;
    server_name votre-domaine.com;
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl;
    server_name votre-domaine.com;
    
    root /var/www/ghost-os;
    index index.php;
    
    ssl_certificate /etc/ssl/certs/votre-domaine.crt;
    ssl_certificate_key /etc/ssl/private/votre-domaine.key;
    
    location / {
        try_files \$uri \$uri/ /index.php?\$args;
    }
    
    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
    }
    
    location /websocket {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

ln -s /etc/nginx/sites-available/ghost-os /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

# Configurer permissions
chown -R www-data:www-data /var/www/ghost-os/uploads
chown -R www-data:www-data /var/www/ghost-os/logs
chmod -R 755 /var/www/ghost-os/uploads
chmod -R 755 /var/www/ghost-os/logs

# Configurer cron
(crontab -l 2>/dev/null; echo "*/5 * * * * php /var/www/ghost-os/cron/check_subscriptions.php") | crontab -
(crontab -l 2>/dev/null; echo "0 * * * * php /var/www/ghost-os/cron/cleanup.php") | crontab -
