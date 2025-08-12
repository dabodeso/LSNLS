@echo off
echo ========================================
echo VERIFICANDO CONEXION A MYSQL
echo ========================================

echo Verificando si MySQL esta ejecutandose en puerto 3306...
netstat -an | findstr :3306 >nul
if %errorlevel% neq 0 (
    echo ERROR: MySQL no parece estar ejecutandose en el puerto 3306
    echo Por favor, inicia MySQL y vuelve a intentar
    pause
    exit /b 1
)

echo MySQL esta ejecutandose en puerto 3306
echo.
echo Intentando conectar a MySQL...
mysql -u root -pcapote -e "SELECT VERSION();" 2>nul
if %errorlevel% neq 0 (
    echo ERROR: No se puede conectar a MySQL
    echo Verifica las credenciales en application.properties:
    echo Usuario: root
    echo Password: capote
    pause
    exit /b 1
)

echo.
echo Verificando si la base de datos 'lsnls' existe...
mysql -u root -pcapote -e "USE lsnls; SHOW TABLES;" 2>nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: La base de datos 'lsnls' no existe
    echo Se creara automaticamente cuando ejecutes la aplicacion
)

echo.
echo ========================================
echo MYSQL ESTA FUNCIONANDO CORRECTAMENTE
echo ========================================
echo Puedes ejecutar la aplicacion LSNLS
pause
