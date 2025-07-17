# Control de Asignación de Cuestionarios - LSNLS

## 🎯 Funcionalidad Implementada

Se ha implementado un **sistema estricto de control de asignación de cuestionarios** que garantiza que cada cuestionario solo pueda ser asignado una vez a jornadas y concursantes, evitando duplicaciones y conflictos.

## 📋 Nuevos Estados de Cuestionarios

### Estados Anteriores
- `borrador` - Cuestionario en desarrollo
- `creado` - Cuestionario terminado y listo para usar
- `adjudicado` - Cuestionario adjudicado para uso específico
- `grabado` - Cuestionario ya utilizado en grabación

### ✨ Nuevos Estados Implementados
- `asignado_jornada` - **Cuestionario asignado automáticamente a una jornada**
- `asignado_concursantes` - **Cuestionario asignado automáticamente a concursantes**

## 🔄 Flujo de Estados Automático

### 1. Asignación a Jornada
```
Estado inicial: creado
↓ (Al asignar a jornada)
Estado automático: asignado_jornada
```

**Cuándo ocurre:**
- Al crear una jornada con cuestionarios seleccionados
- Al editar una jornada y agregar cuestionarios
- **Solo acepta cuestionarios en estado `creado`**

### 2. Asignación a Concursantes  
```
Estado inicial: creado o asignado_jornada
↓ (Al asignar a concursante)
Estado automático: asignado_concursantes
```

**Cuándo ocurre:**
- Al crear un concursante con cuestionario asignado
- Al editar un concursante y asignar cuestionario
- **Acepta cuestionarios en estado `creado` o `asignado_jornada`**

## 🚫 Restricciones y Validaciones

### Restricciones de Asignación

1. **Un cuestionario NO puede asignarse a múltiples jornadas**
   - Estado `asignado_jornada` impide nueva asignación a otras jornadas
   - Error: *"Solo se pueden asignar cuestionarios en estado 'creado'"*

2. **Un cuestionario NO puede asignarse a múltiples concursantes**
   - Estado `asignado_concursantes` impide nueva asignación a otros concursantes  
   - Error: *"Solo se pueden asignar cuestionarios en estado 'creado' o 'asignado_jornada'"*

3. **Los cuestionarios se liberan automáticamente** cuando:
   - Se elimina la jornada → vuelve a `creado`
   - Se quita de la jornada → vuelve a `creado`
   - Se elimina el concursante → vuelve a `creado`
   - Se quita del concursante → vuelve a `creado`

## 🔍 Consultas Disponibles Actualizadas

### Para Jornadas
- Solo muestra cuestionarios en estado `creado`
- Excluye automáticamente los ya asignados

### Para Concursantes  
- Muestra cuestionarios en estado `creado` Y `asignado_jornada`
- Permite reutilizar cuestionarios ya asignados a jornadas
- Excluye los ya asignados a otros concursantes

## 🎨 Interfaz Visual

### Nuevos Badges de Estado
- `Asignado a Jornada` - Badge amarillo con icono de calendario
- `Asignado a Concursantes` - Badge negro con icono de usuarios

### Comportamiento en Botones
- Los cuestionarios en estados de asignación **no muestran botones de cambio manual**
- Solo se pueden liberar eliminando las asignaciones

## 📊 Monitoreo y Consultas

### Estados en la Base de Datos
```sql
-- Ver distribución de estados
SELECT estado, COUNT(*) as cantidad 
FROM cuestionarios 
GROUP BY estado;

-- Ver cuestionarios asignados a jornadas
SELECT c.id, c.estado, j.nombre as jornada
FROM cuestionarios c
JOIN jornadas_cuestionarios jc ON c.id = jc.cuestionario_id  
JOIN jornadas j ON j.id = jc.jornada_id
WHERE c.estado = 'asignado_jornada';

-- Ver cuestionarios asignados a concursantes
SELECT c.id, c.estado, con.nombre as concursante
FROM cuestionarios c
JOIN concursantes con ON c.id = con.cuestionario_id
WHERE c.estado = 'asignado_concursantes';
```

## 🔧 Migración de Datos Existentes

### Script de Migración
Se proporciona `migracion_estados_cuestionarios.sql` que:

1. **Actualiza el schema** agregando los nuevos estados
2. **Migra datos existentes** según las asignaciones actuales
3. **Genera reporte** del estado final

### Proceso de Migración
```bash
# 1. Detener la aplicación
# 2. Ejecutar script de migración
mysql -u usuario -p base_datos < migracion_estados_cuestionarios.sql
# 3. Reiniciar la aplicación
```

## ✅ Beneficios del Sistema

### Para el Equipo
- **Elimina errores** de doble asignación
- **Visibilidad clara** del estado de cada cuestionario
- **Proceso automático** sin intervención manual
- **Trazabilidad completa** de asignaciones

### Para la Producción
- **Garantiza unicidad** de contenidos por grabación
- **Evita conflictos** en jornadas y concursantes
- **Optimiza reutilización** de cuestionarios entre jornadas y concursantes
- **Simplifica gestión** con estados automáticos

## 🚀 Casos de Uso

### Caso 1: Preparación de Jornada
```
1. Crear jornada nueva
2. Seleccionar 5 cuestionarios en estado "creado"
3. ✅ Sistema cambia automáticamente a "asignado_jornada"
4. Esos cuestionarios ya no aparecen para otras jornadas
```

### Caso 2: Asignación a Concursantes
```
1. Cuestionarios "asignado_jornada" siguen disponibles para concursantes
2. Al asignar a concursante → cambia a "asignado_concursantes"  
3. ✅ Ese cuestionario específico ya no se puede asignar a otros concursantes
```

### Caso 3: Liberación Automática
```
1. Se elimina jornada con 5 cuestionarios asignados
2. ✅ Los 5 cuestionarios vuelven automáticamente a estado "creado"
3. Quedan disponibles para nueva asignación
```

## 📞 Soporte

Para dudas o problemas con el sistema de asignación:
- Revisar logs de la aplicación para errores específicos
- Verificar estados en base de datos con las consultas proporcionadas
- Ejecutar script de migración si hay inconsistencias

---

**Implementado exitosamente** ✅ 
**Fecha:** Diciembre 2024
**Versión:** LSNLS v2.0 - Control de Asignación 