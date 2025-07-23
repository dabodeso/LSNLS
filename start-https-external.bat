@echo off
echo ========================================
echo    LSNLS - Inicio HTTPS Externo
echo ========================================
echo.

echo [1/4] Verificando Java 11...
java -version 2>nul
if errorlevel 1 (
    echo ERROR: Java no encontrado. Instala Java 11.
    pause
    exit /b 1
)

echo [2/4] Verificando MySQL...
mysql -u root -p -e "USE lsnls;" 2>nul
if errorlevel 1 (
    echo ERROR: Base de datos 'lsnls' no encontrada.
    echo Ejecuta: CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    pause
    exit /b 1
)

echo [3/4] Verificando certificado SSL...
if not exist "src\main\resources\ssl\keystore.p12" (
    echo ERROR: Certificado SSL no encontrado.
    echo Ejecutando generación automática...
    call generate-ssl-java11.bat
)

echo [4/4] Iniciando aplicación HTTPS...
echo.
echo ========================================
echo    ACCESO A LA APLICACIÓN HTTPS
echo ========================================
echo.
echo Desde la oficina (red local):
echo - https://[IP-LOCAL]:8080
echo.
echo Desde fuera (acceso externo):
echo - https://[IP-PUBLICA]:8088
echo.
echo ⚠️  NOTA: El certificado es autofirmado.
echo    Haz clic en "Avanzado" → "Continuar a [IP] (no seguro)"
echo.
echo ========================================
echo Presiona Ctrl+C para detener
echo.

mvn spring-boot:run 