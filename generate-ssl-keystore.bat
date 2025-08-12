@echo off
echo ========================================
echo GENERANDO KEYSTORE SSL PARA LSNLS
echo ========================================

echo Creando directorio ssl si no existe...
if not exist "src\main\resources\ssl" mkdir "src\main\resources\ssl"

echo.
echo Generando keystore SSL...
keytool -genkeypair -alias lsnls -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src\main\resources\ssl\keystore.p12 -validity 3650 -storepass lsnls2024 -keypass lsnls2024 -dname "CN=localhost, OU=LSNLS, O=LSNLS, L=Madrid, S=Madrid, C=ES"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo KEYSTORE SSL GENERADO EXITOSAMENTE
    echo ========================================
    echo Archivo: src\main\resources\ssl\keystore.p12
    echo Contraseña: lsnls2024
    echo Alias: lsnls
    echo.
    echo El certificado es autofirmado y valido para desarrollo
    echo Para produccion, usa un certificado de una CA reconocida
) else (
    echo.
    echo ERROR: No se pudo generar el keystore SSL
    echo Verifica que Java este instalado y en el PATH
)

pause
