# 🚀 **PASOS PARA ACTIVAR LAS PROTECCIONES DE CONCURRENCIA**

## ⚡ **ACCIÓN INMEDIATA REQUERIDA**

Para que las protecciones implementadas tomen efecto, debes seguir estos pasos **EN ORDEN**:

---

## 📊 **PASO 1: Migración de Base de Datos**

```sql
-- Ejecuta este script SQL en tu base de datos:
-- Archivo: migracion_versionado_prioridad1.sql

ALTER TABLE programas ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE configuracion_global ADD COLUMN version BIGINT DEFAULT 0;

UPDATE programas SET version = 0 WHERE version IS NULL;
UPDATE configuracion_global SET version = 0 WHERE version IS NULL;

ALTER TABLE programas ALTER COLUMN version SET NOT NULL;
ALTER TABLE configuracion_global ALTER COLUMN version SET NOT NULL;
```

**✅ Verificación**: Comprueba que las columnas existen:
```sql
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME IN ('programas', 'configuracion_global') 
AND COLUMN_NAME = 'version';
```

---

## ⚙️ **PASO 2: Recompilar Aplicación**

```bash
# En el directorio raíz del proyecto (donde está pom.xml):
mvn clean compile
```

**🎯 Resultado esperado**: Compilación exitosa sin errores

---

## 🔄 **PASO 3: Reiniciar Aplicación**

```bash
# Detener la aplicación actual (Ctrl+C)
# Luego reiniciar:
mvn spring-boot:run
```

**✅ Verificar en logs**: Busca mensajes como:
- `version column detected for entity Programa`
- `version column detected for entity ConfiguracionGlobal`

---

## 🧪 **PASO 4: Prueba de Verificación**

### **Prueba 1: Optimistic Locking**
1. Abre 2 navegadores
2. Edita el mismo programa en ambos
3. Guarda uno → ✅ Éxito
4. Guarda el otro → ⚠️ Debe mostrar error 409 "modificado por otro usuario"

### **Prueba 2: Números de Concursante**
1. Crea múltiples concursantes rápidamente
2. ✅ Todos deben tener números únicos secuenciales

### **Prueba 3: Asignación de Jornadas**
1. Intenta asignar el mismo cuestionario a 2 jornadas simultáneamente
2. ✅ Solo una debe tener éxito

---

## 📋 **CHECKLIST DE ACTIVACIÓN**

- [ ] **Base de Datos**: Migración ejecutada exitosamente
- [ ] **Compilación**: `mvn clean compile` sin errores
- [ ] **Reinicio**: Aplicación reiniciada con logs de versioning
- [ ] **Pruebas**: Verificación de funcionamiento
- [ ] **Frontend**: Mensajes de error 409 se muestran correctamente

---

## 🆘 **En caso de problemas:**

### **Error: Column already exists**
```sql
-- Si la columna ya existe, solo inicializa:
UPDATE programas SET version = 0 WHERE version IS NULL;
UPDATE configuracion_global SET version = 0 WHERE version IS NULL;
```

### **Error: Compilation Failed**
```bash
# Limpiar completamente y recompilar:
mvn clean
mvn compile
```

### **Error: No version column detected**
- Verifica que la migración SQL se ejecutó correctamente
- Reinicia la aplicación
- Revisa que las entidades tienen `@Version` en el código

---

## 🎯 **CONFIRMACIÓN FINAL**

Una vez completados todos los pasos, tu aplicación estará **PROTEGIDA** contra:

✅ Pérdida de datos por sobrescritura concurrente
✅ Estados inconsistentes en cuestionarios/combos  
✅ Números de concursante duplicados
✅ Usuarios duplicados en registro
✅ Conflictos de concurrencia sin notificación

**🏆 Tu aplicación LSNLS ahora es SEGURA para uso multi-usuario en producción** 