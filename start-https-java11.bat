@echo off
echo ========================================
echo INICIANDO LSNLS CON HTTPS (JAVA 11)
echo ========================================
echo.

REM Verificar que existe el certificado SSL
if not exist "src\main\resources\ssl\keystore.p12" (
    echo ❌ ERROR: No se encontró el certificado SSL
    echo.
    echo Ejecuta primero: generate-ssl-java11.bat
    echo.
    pause
    exit /b 1
)

REM Verificar que Java 11 está disponible
java -version 2>&1 | findstr "11" >nul
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  ADVERTENCIA: Java 11 no detectado
    echo    Verificando versión actual...
    java -version
    echo.
    echo La aplicación puede no funcionar correctamente con otras versiones de Java.
    echo.
    pause
)

echo 🚀 Iniciando aplicación LSNLS...
echo.
echo 📍 Puerto: 8080
echo 🔒 Protocolo: HTTPS
echo 🌐 URL: https://localhost:8080
echo.
echo ⚠️  NOTA: El navegador mostrará una advertencia de seguridad
echo    porque es un certificado autofirmado. Haz clic en "Avanzado"
echo    y luego en "Continuar a localhost (no seguro)".
echo.

REM Iniciar la aplicación
mvn spring-boot:run

echo.
echo ✅ Aplicación detenida
pause 