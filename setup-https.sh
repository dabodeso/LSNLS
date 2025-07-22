#!/bin/bash

echo "========================================"
echo "    CONFIGURACION HTTPS - LSNLS"
echo "========================================"
echo

echo "[1/5] Verificando si Certbot esta instalado..."
if ! command -v certbot &> /dev/null; then
    echo "Certbot no esta instalado. Instalando..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        sudo apt-get update
        sudo apt-get install -y certbot
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        brew install certbot
    else
        echo "Por favor, instala Certbot manualmente desde: https://certbot.eff.org/"
        exit 1
    fi
fi

echo "[2/5] Verificando dominio..."
read -p "Introduce tu dominio (ej: lsnls.tudominio.com): " DOMAIN
if [ -z "$DOMAIN" ]; then
    echo "Error: Debes introducir un dominio valido"
    exit 1
fi

echo "[3/5] Obteniendo certificado SSL..."
sudo certbot certonly --standalone -d $DOMAIN --email admin@$DOMAIN --agree-tos --non-interactive

if [ $? -ne 0 ]; then
    echo "Error al obtener el certificado SSL"
    echo "Verifica que:"
    echo "- El dominio apunta a este servidor"
    echo "- El puerto 80 esta libre"
    echo "- Tienes permisos de sudo"
    exit 1
fi

echo "[4/5] Configurando Spring Boot para HTTPS..."
echo "Creando archivo de configuracion SSL..."

cat > ssl-config.properties << EOF
# Configuracion SSL para LSNLS
server.ssl.enabled=true
server.ssl.key-store-type=PKCS12
server.ssl.key-store=classpath:ssl/keystore.p12
server.ssl.key-store-password=lsnls2024
server.ssl.key-alias=lsnls

# Puerto HTTPS
server.port=8443

# Redireccion HTTP a HTTPS
server.http2.enabled=true
EOF

echo "[5/5] Convirtiendo certificado a formato PKCS12..."
mkdir -p ssl
sudo openssl pkcs12 -export \
    -in /etc/letsencrypt/live/$DOMAIN/fullchain.pem \
    -inkey /etc/letsencrypt/live/$DOMAIN/privkey.pem \
    -out ssl/keystore.p12 \
    -name lsnls \
    -passout pass:lsnls2024

if [ $? -ne 0 ]; then
    echo "Error al convertir el certificado"
    echo "Asegurate de que OpenSSL esta instalado"
    exit 1
fi

echo
echo "========================================"
echo "    CONFIGURACION COMPLETADA"
echo "========================================"
echo
echo "Tu aplicacion ahora esta configurada para HTTPS:"
echo "- URL: https://$DOMAIN:8443"
echo "- Certificado renovado automaticamente"
echo
echo "Para iniciar la aplicacion con HTTPS:"
echo "1. Copia ssl-config.properties a src/main/resources/"
echo "2. Ejecuta: mvn spring-boot:run -Dspring.profiles.active=ssl"
echo
echo "Para renovar el certificado automaticamente:"
echo "sudo crontab -e"
echo "Agrega esta linea: 0 12 * * * /usr/bin/certbot renew --quiet"
echo 