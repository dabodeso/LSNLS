@echo off
echo ========================================
echo GENERANDO CERTIFICADO SSL PARA JAVA 11
echo ========================================
echo.

REM Crear directorio ssl si no existe
if not exist "src\main\resources\ssl" mkdir "src\main\resources\ssl"

REM Eliminar keystore existente si existe
if exist "src\main\resources\ssl\keystore.p12" (
    echo Eliminando keystore existente...
    del "src\main\resources\ssl\keystore.p12"
)

REM Generar certificado SSL compatible con Java 11
echo Generando certificado SSL...
keytool -genkeypair ^
  -alias lsnls ^
  -keyalg RSA ^
  -keysize 2048 ^
  -storetype PKCS12 ^
  -keystore "src\main\resources\ssl\keystore.p12" ^
  -validity 365 ^
  -storepass lsnls2024 ^
  -keypass lsnls2024 ^
  -dname "CN=localhost, OU=LSNLS, O=LSNLS, L=Madrid, S=Madrid, C=ES"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ CERTIFICADO SSL GENERADO EXITOSAMENTE
    echo.
    echo 📁 Ubicación: src\main\resources\ssl\keystore.p12
    echo 🔑 Contraseña: lsnls2024
    echo 🏷️  Alias: lsnls
    echo.
    echo 🌐 La aplicación estará disponible en: https://localhost:8080
    echo.
    echo ⚠️  NOTA: Este es un certificado autofirmado.
    echo    El navegador mostrará una advertencia de seguridad.
    echo    Para producción, usa Let's Encrypt o un certificado válido.
    echo.
) else (
    echo.
    echo ❌ ERROR al generar el certificado SSL
    echo.
)

pause 