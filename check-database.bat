@echo off
echo ========================================
echo VERIFICANDO ESTADO DE LA BASE DE DATOS
echo ========================================

echo Verificando conexion a MySQL...
mysql -u root -pcapote -e "USE lsnls; SELECT 'Conexion exitosa' as Status;" 2>nul
if %errorlevel% neq 0 (
    echo ERROR: No se puede conectar a la base de datos 'lsnls'
    echo La base de datos no existe o hay problemas de conexion
    pause
    exit /b 1
)

echo.
echo ========================================
echo INFORMACION DE LA BASE DE DATOS
echo ========================================

echo.
echo Tablas existentes:
mysql -u root -pcapote -e "USE lsnls; SHOW TABLES;" 2>nul

echo.
echo Conteo de registros por tabla:
mysql -u root -pcapote -e "USE lsnls; SELECT 'usuarios' as tabla, COUNT(*) as total FROM usuarios UNION ALL SELECT 'preguntas', COUNT(*) FROM preguntas UNION ALL SELECT 'cuestionarios', COUNT(*) FROM cuestionarios UNION ALL SELECT 'combos', COUNT(*) FROM combos UNION ALL SELECT 'programas', COUNT(*) FROM programas UNION ALL SELECT 'configuracion_global', COUNT(*) FROM configuracion_global;" 2>nul

echo.
echo Usuarios disponibles:
mysql -u root -pcapote -e "USE lsnls; SELECT nombre, rol FROM usuarios;" 2>nul

echo.
echo ========================================
echo ESTADO DE LA BASE DE DATOS
echo ========================================
echo Si no hay datos, ejecuta: init-database.bat
echo Si hay datos, ejecuta: start-lsnls.bat
echo.
pause
