# 🚀 Recrear Base de Datos desde Cero

## 📋 **Pasos Rápidos:**

### 1️⃣ **En MySQL Workbench:**
```sql
DROP DATABASE IF EXISTS lsnls;
```

### 2️⃣ **En Terminal/CMD:**
```bash
cd lsnls
mvn spring-boot:run
```

## ✅ **¿Qué pasará automáticamente?**

1. **Spring Boot detecta** que no existe la BD `lsnls`
2. **Ejecuta `schema.sql`** → Crea toda la estructura con campos nuevos:
   - ✅ `tematica VARCHAR(100)`
   - ✅ `notas_direccion TEXT`
3. **Ejecuta `data.sql`** → Inserta datos de ejemplo:
   - ✅ 5 usuarios (admin, consulta, guion, verificacion, direccion)
   - ✅ 30 preguntas de diferentes niveles
   - ✅ 3 cuestionarios con temáticas: Genérico, Musical, Navidad
   - ✅ 1 combo de ejemplo

## 🎯 **Resultado:**
- ✅ Base de datos limpia y actualizada
- ✅ Campos nuevos incluidos desde el inicio
- ✅ Datos de prueba para trabajar inmediatamente
- ✅ Sin necesidad de migraciones

## 🔍 **Verificar que todo funciona:**

1. **Login:** usuario `admin` / password `admin`
2. **Ir a Cuestionarios** → Deberías ver la columna "Temática"
3. **Crear nuevo cuestionario** → Selector de temática disponible
4. **Ver cuestionario existente** → Campo "Notas de Dirección" visible

## ⚠️ **Importante:**
- Todos los datos anteriores se **PERDERÁN**
- Los usuarios y passwords vuelven a los valores por defecto
- Si tienes datos importantes, haz backup antes 