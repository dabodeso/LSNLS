@echo off
echo ========================================
echo    ABRIENDO LSNLS EN HTTPS
echo ========================================
echo.
echo URL: https://localhost:8443
echo.
echo NOTA: El navegador mostrara una advertencia de seguridad
echo porque estamos usando un certificado autofirmado.
echo.
echo Para continuar:
echo 1. Haz clic en "Avanzado"
echo 2. Haz clic en "Continuar a localhost (no seguro)"
echo.
pause

start https://localhost:8443 