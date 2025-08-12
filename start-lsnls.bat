@echo off
echo ========================================
echo INICIANDO LSNLS - SISTEMA COMPLETO
echo ========================================

echo Paso 1: Verificando MySQL...
call check-mysql.bat
if %errorlevel% neq 0 (
    echo ERROR: Problema con MySQL. Revisa los errores anteriores.
    pause
    exit /b 1
)

echo.
echo Paso 1.5: Verificando si la base de datos tiene datos...
mysql -u root -pcapote -e "USE lsnls; SELECT COUNT(*) as total FROM usuarios;" 2>nul | findstr "0" >nul
if %errorlevel% equ 0 (
    echo ADVERTENCIA: La base de datos parece estar vacia
    echo Ejecuta init-database.bat para inicializar con datos de prueba
    echo O continua si quieres una base de datos vacia
    echo.
    set /p continue="¿Continuar con base de datos vacia? (s/N): "
    if /i not "%continue%"=="s" (
        echo Operacion cancelada.
        pause
        exit /b 0
    )
)

echo.
echo Paso 2: Verificando keystore SSL...
if not exist "src\main\resources\ssl\keystore.p12" (
    echo Generando keystore SSL...
    call generate-ssl-keystore.bat
    if %errorlevel% neq 0 (
        echo ERROR: No se pudo generar el keystore SSL
        pause
        exit /b 1
    )
) else (
    echo Keystore SSL encontrado
)

echo.
echo Paso 3: Compilando proyecto...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ERROR: Error al compilar el proyecto
    pause
    exit /b 1
)

echo.
echo Paso 4: Iniciando aplicacion LSNLS con HTTPS...
echo URL: https://localhost:8080
echo.
echo NOTA: Como es un certificado autofirmado, el navegador mostrara una advertencia
echo Haz clic en "Avanzado" y luego "Continuar" para acceder a la aplicacion
echo.

java -jar target/lsnls-1.0-SNAPSHOT.jar

pause
