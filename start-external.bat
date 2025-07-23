@echo off
echo ========================================
echo    LSNLS - Inicio Acceso Externo
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

echo [3/4] Obteniendo IP del servidor...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set IP=%%a
    set IP=!IP: =!
    goto :found_ip
)
:found_ip
echo IP del servidor: !IP!

echo [4/4] Iniciando aplicación...
echo.
echo ========================================
echo    ACCESO A LA APLICACIÓN
echo ========================================
echo.
echo Desde la oficina (red local):
echo - http://!IP!:8080
echo.
echo Desde fuera (acceso externo):
echo - http://[IP-PUBLICA]:8088
echo.
echo ========================================
echo Presiona Ctrl+C para detener
echo.

mvn spring-boot:run 