@echo off
echo ========================================
echo    CONFIGURACION HTTPS - LSNLS
echo ========================================
echo.

echo [1/5] Verificando si Certbot esta instalado...
certbot --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Certbot no esta instalado. Instalando...
    echo Por favor, descarga e instala Certbot desde: https://certbot.eff.org/
    echo O ejecuta: pip install certbot
    pause
    exit /b 1
)

echo [2/5] Verificando dominio...
set /p DOMAIN="Introduce tu dominio (ej: lsnls.tudominio.com): "
if "%DOMAIN%"=="" (
    echo Error: Debes introducir un dominio valido
    pause
    exit /b 1
)

echo [3/5] Obteniendo certificado SSL...
certbot certonly --standalone -d %DOMAIN% --email admin@%DOMAIN% --agree-tos --non-interactive

if %errorlevel% neq 0 (
    echo Error al obtener el certificado SSL
    echo Verifica que:
    echo - El dominio apunta a este servidor
    echo - El puerto 80 esta libre
    echo - Tienes acceso de administrador
    pause
    exit /b 1
)

echo [4/5] Configurando Spring Boot para HTTPS...
echo Creando archivo de configuracion SSL...

(
echo # Configuracion SSL para LSNLS
echo server.ssl.enabled=true
echo server.ssl.key-store-type=PKCS12
echo server.ssl.key-store=classpath:ssl/keystore.p12
echo server.ssl.key-store-password=lsnls2024
echo server.ssl.key-alias=lsnls
echo.
echo # Puerto HTTPS
echo server.port=8443
echo.
echo # Redireccion HTTP a HTTPS
echo server.http2.enabled=true
) > ssl-config.properties

echo [5/5] Convirtiendo certificado a formato PKCS12...
openssl pkcs12 -export -in C:\Certbot\live\%DOMAIN%\fullchain.pem -inkey C:\Certbot\live\%DOMAIN%\privkey.pem -out ssl/keystore.p12 -name lsnls -passout pass:lsnls2024

if %errorlevel% neq 0 (
    echo Error al convertir el certificado
    echo Asegurate de que OpenSSL esta instalado
    pause
    exit /b 1
)

echo.
echo ========================================
echo    CONFIGURACION COMPLETADA
echo ========================================
echo.
echo Tu aplicacion ahora esta configurada para HTTPS:
echo - URL: https://%DOMAIN%:8443
echo - Certificado renovado automaticamente
echo.
echo Para iniciar la aplicacion con HTTPS:
echo 1. Copia ssl-config.properties a src/main/resources/
echo 2. Ejecuta: mvn spring-boot:run -Dspring.profiles.active=ssl
echo.
pause 