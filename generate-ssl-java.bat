@echo off
echo ========================================
echo    CERTIFICADO SSL CON JAVA KEYTOOL
echo    Para desarrollo local
echo ========================================
echo.

echo [1/3] Verificando Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java no esta instalado o no esta en el PATH
    pause
    exit /b 1
)

echo [2/3] Creando directorio ssl...
if not exist "ssl" mkdir ssl
if not exist "src\main\resources\ssl" mkdir "src\main\resources\ssl"

echo [3/3] Generando certificado SSL con Java KeyTool...
keytool -genkeypair -alias lsnls -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore ssl/keystore.p12 -validity 365 -storepass lsnls2024 -keypass lsnls2024 -dname "CN=localhost, OU=IT, O=LSNLS, L=Madrid, ST=Madrid, C=ES"

if %errorlevel% neq 0 (
    echo Error al generar el certificado
    pause
    exit /b 1
)

echo [4/4] Copiando certificado a resources...
copy ssl\keystore.p12 src\main\resources\ssl\ >nul

echo.
echo ========================================
echo    CERTIFICADO GENERADO EXITOSAMENTE
echo ========================================
echo.
echo Archivos creados:
echo - ssl/keystore.p12 (certificado PKCS12)
echo - src/main/resources/ssl/keystore.p12 (copiado)
echo.
echo Configuracion SSL:
echo - Alias: lsnls
echo - Contraseña: lsnls2024
echo - Puerto: 8443
echo.
echo Para iniciar la aplicacion con HTTPS:
echo 1. Ejecuta: start-https.bat
echo 2. Accede a: https://localhost:8443
echo.
echo NOTA: Este certificado es autofirmado y el navegador
echo mostrara una advertencia de seguridad. Es normal en desarrollo.
echo Haz clic en "Avanzado" y luego "Continuar a localhost"
echo.
pause 