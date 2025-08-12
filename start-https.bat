@echo off
echo ========================================
echo INICIANDO LSNLS CON HTTPS
echo ========================================

echo Verificando keystore SSL...
if not exist "src\main\resources\ssl\keystore.p12" (
    echo ERROR: No se encuentra el keystore SSL
    echo Ejecuta primero: generate-ssl-keystore.bat
    pause
    exit /b 1
)

echo.
echo Limpiando y compilando el proyecto...
call mvn clean compile -q

echo.
echo Iniciando la aplicacion con HTTPS...
echo URL: https://localhost:8080
echo.
echo NOTA: Como es un certificado autofirmado, el navegador mostrara una advertencia
echo Haz clic en "Avanzado" y luego "Continuar" para acceder a la aplicacion
echo.

java -jar target/lsnls-1.0-SNAPSHOT.jar

pause
