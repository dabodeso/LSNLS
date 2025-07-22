@echo off
echo ========================================
echo CONFIGURACIÓN HTTPS PARA SERVIDOR
echo ========================================
echo.

REM Verificar que estamos en el directorio correcto
if not exist "pom.xml" (
    echo ❌ ERROR: No se encontró pom.xml
    echo    Ejecuta este script desde el directorio raíz del proyecto.
    echo.
    pause
    exit /b 1
)

echo 🔧 PASO 1: Generando certificado SSL...
call generate-ssl-java11.bat

if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERROR al generar el certificado SSL
    pause
    exit /b 1
)

echo.
echo 🔧 PASO 2: Compilando aplicación...
mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERROR al compilar la aplicación
    pause
    exit /b 1
)

echo.
echo 🔧 PASO 3: Creando JAR ejecutable...
mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo ❌ ERROR al crear el JAR
    pause
    exit /b 1
)

echo.
echo ✅ CONFIGURACIÓN COMPLETADA
echo.
echo 📁 Archivos generados:
echo    - src\main\resources\ssl\keystore.p12 (certificado SSL)
echo    - target\lsnls-1.0-SNAPSHOT.jar (aplicación)
echo.
echo 🚀 Para iniciar la aplicación:
echo    1. java -jar target\lsnls-1.0-SNAPSHOT.jar
echo    2. O ejecuta: start-https-java11.bat
echo.
echo 🌐 URL de acceso: https://localhost:8080
echo.
echo ⚠️  NOTA: El navegador mostrará una advertencia de seguridad
echo    porque es un certificado autofirmado.
echo.
pause 