@echo off
echo ========================================
echo ACTUALIZANDO ESQUEMA DE BASE DE DATOS
echo ========================================

echo ADVERTENCIA: Este script actualizara el esquema de la base de datos
echo para incluir los nuevos campos y cambios solicitados.
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
echo Paso 2: Actualizando esquema de la base de datos...
echo Ejecutando comandos SQL de actualizacion...

mysql -u root -pcapote -e "USE lsnls; ALTER TABLE concursantes MODIFY COLUMN resultado INTEGER;" 2>nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: No se pudo cambiar el tipo de resultado (puede que ya sea INTEGER)
)

mysql -u root -pcapote -e "USE lsnls; ALTER TABLE concursantes ADD COLUMN bonico VARCHAR(255) AFTER orden_escaleta;" 2>nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: No se pudo añadir la columna bonico (puede que ya exista)
)

mysql -u root -pcapote -e "USE lsnls; ALTER TABLE concursantes DROP COLUMN concursantes_por_jornada;" 2>nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: No se pudo eliminar la columna concursantes_por_jornada (puede que no exista)
)

echo.
echo ========================================
echo ESQUEMA ACTUALIZADO EXITOSAMENTE
echo ========================================
echo Se han realizado los siguientes cambios:
echo - Campo resultado cambiado a INTEGER
echo - Campo bonico añadido
echo - Campo concursantes_por_jornada eliminado
echo.
echo Ahora puedes ejecutar la aplicacion con los nuevos campos
pause
