#!/bin/bash

echo "========================================"
echo "    CERTIFICADO SSL AUTOFIRMADO"
echo "    Para desarrollo local"
echo "========================================"
echo

echo "[1/3] Creando directorio ssl..."
mkdir -p ssl

echo "[2/3] Generando certificado SSL autofirmado..."
openssl req -x509 -newkey rsa:4096 -keyout ssl/key.pem -out ssl/cert.pem -days 365 -nodes -subj "/C=ES/ST=Madrid/L=Madrid/O=LSNLS/OU=IT/CN=localhost"

if [ $? -ne 0 ]; then
    echo "Error al generar el certificado"
    echo "Asegurate de que OpenSSL esta instalado"
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Instala con: sudo apt-get install openssl"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "Instala con: brew install openssl"
    fi
    exit 1
fi

echo "[3/3] Convirtiendo a formato PKCS12 para Spring Boot..."
openssl pkcs12 -export -in ssl/cert.pem -inkey ssl/key.pem -out ssl/keystore.p12 -name lsnls -passout pass:lsnls2024

if [ $? -ne 0 ]; then
    echo "Error al convertir el certificado"
    exit 1
fi

echo
echo "========================================"
echo "    CERTIFICADO GENERADO"
echo "========================================"
echo
echo "Archivos creados:"
echo "- ssl/cert.pem (certificado)"
echo "- ssl/key.pem (clave privada)"
echo "- ssl/keystore.p12 (formato Spring Boot)"
echo
echo "Para usar en desarrollo:"
echo "1. Copia ssl/keystore.p12 a src/main/resources/ssl/"
echo "2. Ejecuta: ./start-https.sh"
echo "3. Accede a https://localhost:8443"
echo
echo "NOTA: Este certificado es autofirmado y el navegador"
echo "mostrara una advertencia de seguridad. Es normal en desarrollo."
echo

# Copiar automáticamente el keystore a resources
mkdir -p src/main/resources/ssl
cp ssl/keystore.p12 src/main/resources/ssl/
echo "✓ Certificado copiado a src/main/resources/ssl/keystore.p12" 