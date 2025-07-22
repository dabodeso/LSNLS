@echo off
echo ========================================
echo    INICIANDO LSNLS CON HTTPS
echo ========================================
echo.

echo [1/3] Verificando certificado SSL...
if not exist "src\main\resources\ssl\keystore.p12" (
    echo Error: No se encuentra el certificado SSL
    echo Ejecuta primero: generate-self-signed-cert.bat
    pause
    exit /b 1
)

echo [2/3] Compilando aplicacion...
call mvn clean compile

if %errorlevel% neq 0 (
    echo Error al compilar la aplicacion
    pause
    exit /b 1
)

echo [3/3] Iniciando aplicacion con HTTPS...
echo.
echo La aplicacion se iniciara en: https://localhost:8443
echo.
echo NOTA: Si usas un certificado autofirmado, el navegador
echo mostrara una advertencia de seguridad. Haz clic en
echo "Avanzado" y luego "Continuar a localhost (no seguro)"
echo.
pause

call mvn spring-boot:run -Dspring.profiles.active=ssl 