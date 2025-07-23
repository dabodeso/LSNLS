@echo off
echo ========================================
echo    OBTENER IP PÚBLICA DEL SERVIDOR
echo ========================================
echo.

echo Obteniendo IP pública...
echo.

echo [1/2] IP Local del servidor:
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set IP=%%a
    set IP=!IP: =!
    echo - !IP!
)

echo.
echo [2/2] IP Pública del servidor:
powershell -Command "(Invoke-WebRequest -uri 'http://ifconfig.me/ip' -UseBasicParsing).Content"

echo.
echo ========================================
echo    CONFIGURACIÓN DE ACCESO
echo ========================================
echo.
echo Para acceso desde la oficina:
echo - http://[IP-LOCAL]:8080
echo.
echo Para acceso desde fuera:
echo - http://[IP-PUBLICA]:8088
echo.
echo ========================================
pause 