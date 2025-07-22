#!/bin/bash

echo "========================================"
echo "    INICIANDO LSNLS CON HTTPS"
echo "========================================"
echo

echo "[1/3] Verificando certificado SSL..."
if [ ! -f "src/main/resources/ssl/keystore.p12" ]; then
    echo "Error: No se encuentra el certificado SSL"
    echo "Ejecuta primero: ./generate-self-signed-cert.sh"
    exit 1
fi

echo "[2/3] Compilando aplicacion..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "Error al compilar la aplicacion"
    exit 1
fi

echo "[3/3] Iniciando aplicacion con HTTPS..."
echo
echo "La aplicacion se iniciara en: https://localhost:8443"
echo
echo "NOTA: Si usas un certificado autofirmado, el navegador"
echo "mostrara una advertencia de seguridad. Haz clic en"
echo "'Avanzado' y luego 'Continuar a localhost (no seguro)'"
echo

mvn spring-boot:run -Dspring.profiles.active=ssl 