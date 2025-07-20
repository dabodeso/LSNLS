@echo off
REM ========================================
REM SCRIPT DE BACKUP AUTOMÁTICO - LSNLS
REM ========================================
REM Ejecutar diariamente a las 22:00 (10 PM) hora española
REM Mantiene backups durante 1 semana

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
set LOG_DIR=%~dp0logs

REM Crear directorios si no existen
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM ========================================
REM FECHA Y HORA
REM ========================================
for /f "tokens=1-4 delims=/ " %%a in ('date /t') do (
    set TODAY=%%d%%b%%c
)

for /f "tokens=1-2 delims=: " %%a in ('time /t') do (
    set NOW=%%a%%b
)

set TIMESTAMP=%TODAY%_%NOW%
set BACKUP_FILE=%BACKUP_DIR%\lsnls_backup_%TIMESTAMP%.sql
set LOG_FILE=%LOG_DIR%\backup_%TIMESTAMP%.log

REM ========================================
REM INICIO DEL BACKUP
REM ========================================
echo [%date% %time%] ======================================== >> "%LOG_FILE%"
echo [%date% %time%] INICIANDO BACKUP AUTOMÁTICO LSNLS >> "%LOG_FILE%"
echo [%date% %time%] Base de datos: %DB_NAME% >> "%LOG_FILE%"
echo [%date% %time%] Archivo: %BACKUP_FILE% >> "%LOG_FILE%"
echo [%date% %time%] ======================================== >> "%LOG_FILE%"

REM ========================================
REM EJECUTAR BACKUP
REM ========================================
echo [%date% %time%] Ejecutando mysqldump... >> "%LOG_FILE%"

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
    %DB_NAME% > "%BACKUP_FILE%" 2>> "%LOG_FILE%"

REM ========================================
REM VERIFICAR RESULTADO
REM ========================================
if %errorlevel% equ 0 (
    echo [%date% %time%] ✅ BACKUP COMPLETADO EXITOSAMENTE >> "%LOG_FILE%"
    echo [%date% %time%] Archivo creado: %BACKUP_FILE% >> "%LOG_FILE%"
    
    REM Obtener tamaño del archivo
    for %%A in ("%BACKUP_FILE%") do set BACKUP_SIZE=%%~zA
    echo [%date% %time%] Tamaño del backup: %BACKUP_SIZE% bytes >> "%LOG_FILE%"
    
    REM Comprimir backup (opcional)
    echo [%date% %time%] Comprimiendo backup... >> "%LOG_FILE%"
    powershell -command "Compress-Archive -Path '%BACKUP_FILE%' -DestinationPath '%BACKUP_FILE%.zip' -Force" 2>> "%LOG_FILE%"
    
    if exist "%BACKUP_FILE%.zip" (
        echo [%date% %time%] ✅ Backup comprimido: %BACKUP_FILE%.zip >> "%LOG_FILE%"
        del "%BACKUP_FILE%"
    )
    
) else (
    echo [%date% %time%] ❌ ERROR EN EL BACKUP >> "%LOG_FILE%"
    echo [%date% %time%] Código de error: %errorlevel% >> "%LOG_FILE%"
)

REM ========================================
REM LIMPIEZA DE BACKUPS ANTIGUOS (1 SEMANA)
REM ========================================
echo [%date% %time%] Limpiando backups antiguos... >> "%LOG_FILE%"

REM Eliminar archivos más antiguos de 7 días
forfiles /p "%BACKUP_DIR%" /s /m *.sql /d -7 /c "cmd /c del @path" 2>nul
forfiles /p "%BACKUP_DIR%" /s /m *.zip /d -7 /c "cmd /c del @path" 2>nul

REM Eliminar logs antiguos (más de 30 días)
forfiles /p "%LOG_DIR%" /s /m *.log /d -30 /c "cmd /c del @path" 2>nul

echo [%date% %time%] ✅ Limpieza completada >> "%LOG_FILE%"

REM ========================================
REM RESUMEN FINAL
REM ========================================
echo [%date% %time%] ======================================== >> "%LOG_FILE%"
echo [%date% %time%] BACKUP FINALIZADO >> "%LOG_FILE%"
echo [%date% %time%] ======================================== >> "%LOG_FILE%"

REM Mostrar resumen en consola
echo.
echo ========================================
echo BACKUP LSNLS COMPLETADO
echo ========================================
echo Fecha: %date% %time%
echo Archivo: %BACKUP_FILE%
echo Log: %LOG_FILE%
echo ========================================

endlocal 