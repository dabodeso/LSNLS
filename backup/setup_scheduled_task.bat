@echo off
REM ========================================
REM CONFIGURACIÓN DE TAREA PROGRAMADA - LSNLS
REM ========================================
REM Este script configura una tarea programada para ejecutar
REM el backup automático todos los días a las 22:00 (10 PM)

setlocal enabledelayedexpansion

echo ========================================
echo CONFIGURACIÓN DE BACKUP AUTOMÁTICO LSNLS
echo ========================================
echo.

REM ========================================
REM VERIFICAR PERMISOS DE ADMINISTRADOR
REM ========================================
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Este script requiere permisos de administrador
    echo.
    echo Por favor, ejecute este script como administrador:
    echo 1. Haga clic derecho en este archivo
    echo 2. Seleccione "Ejecutar como administrador"
    echo.
    pause
    exit /b 1
)

echo ✅ Permisos de administrador verificados
echo.

REM ========================================
REM OBTENER RUTA DEL SCRIPT DE BACKUP
REM ========================================
set SCRIPT_PATH=%~dp0backup_database.bat
set SCRIPT_PATH=%SCRIPT_PATH:~0,-1%

if not exist "%SCRIPT_PATH%" (
    echo ❌ ERROR: No se encontró el script de backup
    echo Ruta buscada: %SCRIPT_PATH%
    echo.
    echo Asegúrese de que backup_database.bat esté en el mismo directorio
    echo.
    pause
    exit /b 1
)

echo ✅ Script de backup encontrado: %SCRIPT_PATH%
echo.

REM ========================================
REM NOMBRE DE LA TAREA
REM ========================================
set TASK_NAME=LSNLS_DailyBackup
set TASK_DESCRIPTION="Backup automático diario de la base de datos LSNLS"

REM ========================================
REM ELIMINAR TAREA EXISTENTE SI EXISTE
REM ========================================
echo Eliminando tarea existente si existe...
schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

REM ========================================
REM CREAR NUEVA TAREA PROGRAMADA
REM ========================================
echo Creando nueva tarea programada...

schtasks /create /tn "%TASK_NAME%" ^
    /tr "%SCRIPT_PATH%" ^
    /sc daily ^
    /st 22:00 ^
    /ru "SYSTEM" ^
    /f

if %errorlevel% equ 0 (
    echo ✅ Tarea programada creada exitosamente
    echo.
    echo ========================================
    echo CONFIGURACIÓN COMPLETADA
    echo ========================================
    echo Nombre de la tarea: %TASK_NAME%
    echo Descripción: %TASK_DESCRIPTION%
    echo Programación: Diaria a las 22:00 (10 PM)
    echo Script: %SCRIPT_PATH%
    echo Usuario: SYSTEM (permisos elevados)
    echo ========================================
    echo.
    echo 📋 COMANDOS ÚTILES:
    echo.
    echo Ver tarea programada:
    echo   schtasks /query /tn "%TASK_NAME%"
    echo.
    echo Ejecutar tarea manualmente:
    echo   schtasks /run /tn "%TASK_NAME%"
    echo.
    echo Eliminar tarea:
    echo   schtasks /delete /tn "%TASK_NAME%" /f
    echo.
    echo Ver todas las tareas:
    echo   schtasks /query
    echo ========================================
) else (
    echo ❌ ERROR: No se pudo crear la tarea programada
    echo Código de error: %errorlevel%
    echo.
    echo Posibles soluciones:
    echo 1. Verificar que el script de backup existe
    echo 2. Ejecutar como administrador
    echo 3. Verificar permisos del sistema
    echo.
)

echo.
pause
endlocal 