@echo off
echo ========================================
echo    INICIANDO LSNLS EN PRODUCCION
echo ========================================
echo.

echo [1/4] Verificando certificado SSL de produccion...
if not exist "src\main\resources\ssl\production-keystore.p12" (
    echo Error: No se encuentra el certificado SSL de produccion
    echo Ejecuta primero: setup-production-https.bat
    pause
    exit /b 1
)

echo [2/4] Verificando configuracion de produccion...
if not exist "src\main\resources\application-prod.properties" (
    echo Error: No se encuentra la configuracion de produccion
    pause
    exit /b 1
)

echo [3/4] Compilando aplicacion para produccion...
call mvn clean compile -q

if %errorlevel% neq 0 (
    echo Error al compilar la aplicacion
    pause
    exit /b 1
)

echo [4/4] Iniciando aplicacion en modo produccion...
echo.
echo La aplicacion se iniciara en modo produccion:
echo - Puerto: 443 (HTTPS)
echo - Perfil: prod
echo - Logging: Reducido para mejor rendimiento
echo.
echo Para acceder desde internet:
echo - URL: https://TU_IP_PUBLICA
echo.
echo NOTA: Asegurate de que:
echo - El puerto 443 este abierto en tu firewall
echo - Tu IP publica sea accesible desde internet
echo - El certificado SSL sea valido
echo.
pause

call mvn spring-boot:run -Dspring.profiles.active=prod 