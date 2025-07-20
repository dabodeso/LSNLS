@echo off
REM ========================================
REM SCRIPT DE RESTAURACIÓN - LSNLS
REM ========================================
REM Uso: restore_database.bat [archivo_backup]
REM Ejemplo: restore_database.bat lsnls_backup_20241201_2200.sql

setlocal enabledelayedexpansion

REM ========================================
REM CONFIGURACIÓN
REM ========================================
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=lsnls
set DB_USER=root
set DB_PASSWORD=admin123

REM Directorio de backups
set BACKUP_DIR=%~dp0backups

REM ========================================
REM VERIFICAR PARÁMETROS
REM ========================================
if "%~1"=="" (
    echo.
    echo ========================================
    echo ERROR: Debe especificar un archivo de backup
    echo ========================================
    echo Uso: restore_database.bat [archivo_backup]
    echo.
    echo Archivos disponibles:
    echo ========================================
    dir /b "%BACKUP_DIR%\*.sql" 2>nul
    dir /b "%BACKUP_DIR%\*.zip" 2>nul
    echo ========================================
    echo.
    pause
    exit /b 1
)

set BACKUP_FILE=%~1

REM ========================================
REM VERIFICAR SI EL ARCHIVO EXISTE
REM ========================================
if not exist "%BACKUP_FILE%" (
    REM Intentar buscar en el directorio de backups
    if exist "%BACKUP_DIR%\%BACKUP_FILE%" (
        set BACKUP_FILE=%BACKUP_DIR%\%BACKUP_FILE%
    ) else (
        echo.
        echo ========================================
        echo ERROR: Archivo de backup no encontrado
        echo ========================================
        echo Archivo buscado: %BACKUP_FILE%
        echo.
        echo Archivos disponibles:
        echo ========================================
        dir /b "%BACKUP_DIR%\*.sql" 2>nul
        dir /b "%BACKUP_DIR%\*.zip" 2>nul
        echo ========================================
        echo.
        pause
        exit /b 1
    )
)

REM ========================================
REM CONFIRMAR RESTAURACIÓN
REM ========================================
echo.
echo ========================================
echo RESTAURACIÓN DE BASE DE DATOS LSNLS
echo ========================================
echo Archivo: %BACKUP_FILE%
echo Base de datos: %DB_NAME%
echo Host: %DB_HOST%:%DB_PORT%
echo Usuario: %DB_USER%
echo.
echo ⚠️  ADVERTENCIA: Esta operación sobrescribirá la base de datos actual
echo.
set /p CONFIRM="¿Está seguro de continuar? (s/N): "

if /i not "%CONFIRM%"=="s" (
    echo Restauración cancelada.
    pause
    exit /b 0
)

REM ========================================
REM PREPARAR ARCHIVO DE RESTAURACIÓN
REM ========================================
set RESTORE_FILE=%BACKUP_FILE%

REM Si es un archivo ZIP, descomprimirlo
if "%BACKUP_FILE:~-4%"==".zip" (
    echo Descomprimiendo archivo ZIP...
    set TEMP_DIR=%TEMP%\lsnls_restore_%random%
    mkdir "%TEMP_DIR%" 2>nul
    
    powershell -command "Expand-Archive -Path '%BACKUP_FILE%' -DestinationPath '%TEMP_DIR%' -Force"
    
    REM Buscar el archivo .sql descomprimido
    for %%f in ("%TEMP_DIR%\*.sql") do set RESTORE_FILE=%%f
    
    if not exist "%RESTORE_FILE%" (
        echo ERROR: No se encontró archivo .sql en el ZIP
        rmdir /s /q "%TEMP_DIR%" 2>nul
        pause
        exit /b 1
    )
)

REM ========================================
REM CREAR BACKUP DE SEGURIDAD
REM ========================================
echo.
echo Creando backup de seguridad antes de restaurar...
set SAFETY_BACKUP=%BACKUP_DIR%\safety_backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%.sql

mysqldump -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% ^
    --single-transaction ^
    --routines ^
    --triggers ^
    --events ^
    --add-drop-database ^
    --create-options ^
    --complete-insert ^
    --extended-insert ^
    --set-charset ^
    --default-character-set=utf8mb4 ^
    %DB_NAME% > "%SAFETY_BACKUP%"

if %errorlevel% neq 0 (
    echo ERROR: No se pudo crear el backup de seguridad
    if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%" 2>nul
    pause
    exit /b 1
)

echo ✅ Backup de seguridad creado: %SAFETY_BACKUP%

REM ========================================
REM RESTAURAR BASE DE DATOS
REM ========================================
echo.
echo ========================================
echo RESTAURANDO BASE DE DATOS...
echo ========================================
echo Archivo: %RESTORE_FILE%
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% < "%RESTORE_FILE%"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo ✅ RESTAURACIÓN COMPLETADA EXITOSAMENTE
    echo ========================================
    echo Base de datos: %DB_NAME%
    echo Archivo restaurado: %RESTORE_FILE%
    echo Backup de seguridad: %SAFETY_BACKUP%
    echo ========================================
) else (
    echo.
    echo ========================================
    echo ❌ ERROR EN LA RESTAURACIÓN
    echo ========================================
    echo Código de error: %errorlevel%
    echo.
    echo Para restaurar el backup de seguridad:
    echo mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% ^< "%SAFETY_BACKUP%"
    echo ========================================
)

REM ========================================
REM LIMPIEZA
REM ========================================
if exist "%TEMP_DIR%" (
    echo Limpiando archivos temporales...
    rmdir /s /q "%TEMP_DIR%" 2>nul
)

echo.
pause
endlocal 