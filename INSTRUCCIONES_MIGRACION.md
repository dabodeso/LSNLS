# 🔄 Migración de Base de Datos - Cuestionarios

## ⚠️ **IMPORTANTE: EJECUTAR ANTES DE USAR LA APLICACIÓN**

Los nuevos campos `tematica` y `notas_direccion` deben añadirse a la tabla `cuestionarios` existente.

## 📋 **Pasos a Seguir:**

### 1️⃣ **Verificar Estado Actual**
```sql
-- Ejecutar este script para ver si necesitas la migración
mysql -u [usuario] -p lsnls < verificar_migracion.sql
```

### 2️⃣ **Hacer Backup (RECOMENDADO)**
```bash
mysqldump -u [usuario] -p lsnls > backup_antes_migracion.sql
```

### 3️⃣ **Ejecutar Migración**
```sql
-- Si los campos NO existen, ejecutar:
mysql -u [usuario] -p lsnls < migracion_cuestionarios_simple.sql
```

### 4️⃣ **Verificar Resultado**
Después de la migración, deberías ver:
```
mysql> DESCRIBE cuestionarios;
+------------------+----------+------+-----+---------+----------------+
| Field            | Type     | Null | Key | Default | Extra          |
+------------------+----------+------+-----+---------+----------------+
| id               | bigint   | NO   | PRI | NULL    | auto_increment |
| creacion_usuario_id | bigint | NO   |     | NULL    |                |
| fecha_creacion   | datetime | YES  |     | NULL    |                |
| estado           | enum     | NO   |     | NULL    |                |
| nivel            | enum     | NO   |     | NULL    |                |
| tematica         | varchar(100) | YES |  | NULL    |                |
| notas_direccion  | text     | YES  |     | NULL    |                |
+------------------+----------+------+-----+---------+----------------+
```

## 🚨 **Si Algo Sale Mal:**

### Restaurar Backup:
```bash
mysql -u [usuario] -p lsnls < backup_antes_migracion.sql
```

### Errores Comunes:

**"Column 'tematica' already exists"**
- ✅ Perfecto, el campo ya existe, no hacer nada

**"Table 'cuestionarios' doesn't exist"**
- ❌ Problema: ejecutar primero el `schema.sql` completo

**"Access denied"**
- ❌ Verificar permisos de usuario MySQL

## 🎯 **Resultado Final:**
- ✅ Cuestionarios existentes tendrán `tematica = NULL` (se muestra como "Genérico")
- ✅ Cuestionarios existentes tendrán `notas_direccion = NULL` (campo vacío)  
- ✅ Nuevos cuestionarios podrán usar ambos campos sin problemas
- ✅ La aplicación funcionará correctamente

## 📞 **¿Necesitas Ayuda?**
Si tienes problemas con la migración:
1. Envía el resultado de `DESCRIBE cuestionarios;`
2. Envía cualquier mensaje de error
3. Indica tu versión de MySQL 