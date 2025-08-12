@echo off
echo ========================================
echo INICIALIZANDO BASE DE DATOS LSNLS
echo ========================================

echo ADVERTENCIA: Este script borrara todos los datos existentes
echo y los reemplazara con datos de prueba iniciales.
echo.
set /p confirm="¿Estas seguro? (s/N): "
if /i not "%confirm%"=="s" (
    echo Operacion cancelada.
    pause
    exit /b 0
)

echo.
echo Paso 1: Verificando MySQL...
call check-mysql.bat
if %errorlevel% neq 0 (
    echo ERROR: Problema con MySQL. Revisa los errores anteriores.
    pause
    exit /b 1
)

echo.
echo Paso 2: Limpiando base de datos existente...
mysql -u root -pcapote -e "DROP DATABASE IF EXISTS lsnls; CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if %errorlevel% neq 0 (
    echo ERROR: No se pudo limpiar la base de datos
    pause
    exit /b 1
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
echo Paso 4: Inicializando base de datos con datos de prueba...
echo Ejecutando con modo de inicializacion forzada...
java -jar target/lsnls-1.0-SNAPSHOT.jar --spring.sql.init.mode=always

echo.
echo ========================================
echo BASE DE DATOS INICIALIZADA EXITOSAMENTE
echo ========================================
echo Se han creado:
echo - Usuarios de prueba (admin, consulta, guion, etc.)
echo - 60 preguntas de ejemplo
echo - Cuestionarios y combos de prueba
echo - Configuracion inicial
echo.
echo Ahora puedes ejecutar la aplicacion normalmente
pause
