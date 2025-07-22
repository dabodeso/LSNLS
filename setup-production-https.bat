@echo off
echo ========================================
echo    CONFIGURACION HTTPS PRODUCCION
echo    LSNLS - IP PUBLICA
echo ========================================
echo.

echo [1/6] Verificando si Certbot esta instalado...
certbot --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Certbot no esta instalado. Instalando...
    echo.
    echo OPCIONES DE INSTALACION:
    echo 1. Descargar desde: https://certbot.eff.org/
    echo 2. Usar Chocolatey: choco install certbot
    echo 3. Usar pip: pip install certbot
    echo.
    echo Despues de instalar Certbot, ejecuta este script nuevamente.
    pause
    exit /b 1
)

echo [2/6] Verificando IP publica...
set /p IP_PUBLICA="Introduce tu IP publica (ej: 192.168.1.100): "
if "%IP_PUBLICA%"=="" (
    echo Error: Debes introducir una IP publica valida
    pause
    exit /b 1
)

echo [3/6] Verificando puertos abiertos...
echo Verificando puerto 80...
netstat -an | findstr ":80" >nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: Puerto 80 no esta abierto
    echo Asegurate de que el puerto 80 este disponible para Let's Encrypt
    echo.
)

echo [4/6] Obteniendo certificado SSL con Let's Encrypt...
echo.
echo IMPORTANTE: Para obtener un certificado con IP publica, necesitas:
echo 1. Que tu IP sea accesible desde internet
echo 2. Que el puerto 80 este abierto
echo 3. Que no haya firewall bloqueando las conexiones
echo.
echo Certbot intentara verificar el dominio usando el puerto 80
echo.

certbot certonly --standalone -d %IP_PUBLICA% --email admin@%IP_PUBLICA% --agree-tos --non-interactive

if %errorlevel% neq 0 (
    echo.
    echo ERROR: No se pudo obtener el certificado SSL
    echo.
    echo POSIBLES SOLUCIONES:
    echo 1. Verifica que tu IP sea accesible desde internet
    echo 2. Asegurate de que el puerto 80 este libre
    echo 3. Desactiva temporalmente el firewall
    echo 4. Si tienes un router, configura port forwarding
    echo.
    echo ALTERNATIVA: Usar certificado autofirmado para pruebas
    echo Ejecuta: generate-ssl-java.bat
    pause
    exit /b 1
)

echo [5/6] Configurando certificado para Spring Boot...
echo Creando directorio ssl...
if not exist "ssl" mkdir ssl
if not exist "src\main\resources\ssl" mkdir "src\main\resources\ssl"

echo Convirtiendo certificado a formato PKCS12...
openssl pkcs12 -export -in C:\Certbot\live\%IP_PUBLICA%\fullchain.pem -inkey C:\Certbot\live\%IP_PUBLICA%\privkey.pem -out ssl/production-keystore.p12 -name lsnls-prod -passout pass:lsnls2024

if %errorlevel% neq 0 (
    echo Error al convertir el certificado
    echo Asegurate de que OpenSSL este instalado
    echo Descarga desde: https://slproweb.com/products/Win32OpenSSL.html
    pause
    exit /b 1
)

echo Copiando certificado a resources...
copy ssl\production-keystore.p12 src\main\resources\ssl\ >nul

echo [6/6] Actualizando configuracion de CORS...
echo Actualizando application-prod.properties con tu IP...

powershell -Command "(Get-Content 'src\main\resources\application-prod.properties') -replace 'TU_IP_PUBLICA', '%IP_PUBLICA%' | Set-Content 'src\main\resources\application-prod.properties'"

echo.
echo ========================================
echo    CONFIGURACION COMPLETADA
echo ========================================
echo.
echo Tu aplicacion esta configurada para HTTPS en produccion:
echo - URL: https://%IP_PUBLICA%
echo - Puerto: 443 (HTTPS)
echo - Certificado: Let's Encrypt (valido por 90 dias)
echo.
echo PARA INICIAR EN PRODUCCION:
echo 1. Ejecuta: mvn spring-boot:run -Dspring.profiles.active=prod
echo 2. O crea un JAR: mvn clean package
echo 3. Ejecuta: java -jar target/lsnls-1.0-SNAPSHOT.jar --spring.profiles.active=prod
echo.
echo RENOVACION AUTOMATICA:
echo Para renovar el certificado automaticamente:
echo 1. Abre el Programador de tareas de Windows
echo 2. Crea una tarea que ejecute: certbot renew --quiet
echo 3. Programa la tarea para ejecutarse cada 60 dias
echo.
echo NOTA: Asegurate de que tu IP publica sea estable
echo Si cambia, necesitaras obtener un nuevo certificado
echo.
pause 