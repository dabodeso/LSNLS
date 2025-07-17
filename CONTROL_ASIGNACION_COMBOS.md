# Control de Asignación de Combos - LSNLS

## 🎯 Funcionalidad Implementada

Se ha implementado un **sistema estricto de control de asignación de combos** que garantiza que cada combo solo pueda ser asignado una vez a jornadas y concursantes, evitando duplicaciones y conflictos.

## 📋 Nuevos Estados de Combos

### Estados Anteriores
- `borrador` - Combo en desarrollo
- `creado` - Combo terminado y listo para usar
- `adjudicado` - Combo adjudicado para uso específico
- `grabado` - Combo ya utilizado en grabación

### ✨ Nuevos Estados Implementados
- `asignado_jornada` - **Combo asignado automáticamente a una jornada**
- `asignado_concursantes` - **Combo asignado automáticamente a concursantes**

## 🔄 Flujo de Estados Automático

### 1. Asignación a Jornada
```
Estado inicial: creado
↓ (Al asignar a jornada)
Estado automático: asignado_jornada
```

**Cuándo ocurre:**
- Al crear una jornada con combos seleccionados
- Al editar una jornada y agregar combos
- **Solo acepta combos en estado `creado`**

### 2. Asignación a Concursantes  
```
Estado inicial: creado o asignado_jornada
↓ (Al asignar a concursante)
Estado automático: asignado_concursantes
```

**Cuándo ocurre:**
- Al crear un concursante con combo asignado
- Al editar un concursante y asignar combo
- **Acepta combos en estado `creado` o `asignado_jornada`**

## 🚫 Restricciones y Validaciones

### Restricciones de Asignación

1. **Un combo NO puede asignarse a múltiples jornadas**
   - Estado `asignado_jornada` impide nueva asignación a otras jornadas
   - Error: *"Solo se pueden asignar combos en estado 'creado'"*

2. **Un combo NO puede asignarse a múltiples concursantes**
   - Estado `asignado_concursantes` impide nueva asignación a otros concursantes  
   - Error: *"Solo se pueden asignar combos en estado 'creado' o 'asignado_jornada'"*

3. **Los combos se liberan automáticamente** cuando:
   - Se elimina la jornada → vuelve a `creado`
   - Se quita de la jornada → vuelve a `creado`
   - Se elimina el concursante → vuelve a `creado`
   - Se quita del concursante → vuelve a `creado`

## 🔍 Consultas Disponibles Actualizadas

### Para Jornadas
- Solo muestra combos en estado `creado`
- Excluye automáticamente los ya asignados

### Para Concursantes  
- Muestra combos en estado `creado` Y `asignado_jornada`
- Permite reutilizar combos ya asignados a jornadas
- Excluye los ya asignados a otros concursantes

## 🎨 Interfaz Visual

### Nuevos Badges de Estado
- `Asignado a Jornada` - Badge amarillo con icono de calendario
- `Asignado a Concursantes` - Badge negro con icono de usuarios

### Comportamiento en Botones
- Los combos en estados de asignación **no muestran botones de cambio manual**
- Solo se pueden liberar eliminando las asignaciones

## 📊 Monitoreo y Consultas

### Estados en la Base de Datos
```sql
-- Ver distribución de estados
SELECT estado, COUNT(*) as cantidad 
FROM combos 
GROUP BY estado;

-- Ver combos asignados a jornadas
SELECT c.id, c.estado, j.nombre as jornada
FROM combos c
JOIN jornadas_combos jc ON c.id = jc.combo_id  
JOIN jornadas j ON j.id = jc.jornada_id
WHERE c.estado = 'asignado_jornada';

-- Ver combos asignados a concursantes
SELECT c.id, c.estado, con.nombre as concursante
FROM combos c
JOIN concursantes con ON c.id = con.combo_id
WHERE c.estado = 'asignado_concursantes';
```

## 🔧 Migración de Datos Existentes

### Script de Migración
Ejecutar `migracion_estados_combos.sql` que incluye:

1. **Modificación de ENUM**: Agrega los nuevos estados a la tabla
2. **Verificación**: Muestra asignaciones actuales
3. **Migración automática**: 
   - Combos asignados a concursantes → `asignado_concursantes`
   - Combos asignados solo a jornadas → `asignado_jornada`
4. **Verificación final**: Confirma que la migración fue exitosa

### Orden de Ejecución
```sql
-- 1. Modificar ENUM
ALTER TABLE combos MODIFY COLUMN estado ENUM(...);

-- 2. Migrar combos a concursantes
UPDATE combos c SET estado = 'asignado_concursantes' WHERE c.id IN (...);

-- 3. Migrar combos a jornadas
UPDATE combos c SET estado = 'asignado_jornada' WHERE c.id IN (...);
```

## ⚡ Beneficios del Sistema

### Para el Equipo de Producción
- **Evita errores**: No más combos duplicados en múltiples concursantes
- **Control visual**: Estados claros y badges informativos
- **Flujo automático**: Cambios de estado sin intervención manual
- **Trazabilidad**: Historial completo de asignaciones

### Para el Sistema
- **Integridad de datos**: Garantiza consistencia entre tablas
- **Prevención de conflictos**: Validaciones estrictas en backend
- **Liberación automática**: Limpieza automática al quitar asignaciones
- **Escalabilidad**: Sistema preparado para futuros requerimientos

## 🚀 Casos de Uso

### Flujo Normal
1. Se crea un combo → Estado: `creado`
2. Se asigna a jornada → Estado: `asignado_jornada` 
3. Se asigna a concursante → Estado: `asignado_concursantes`
4. Se elimina concursante → Estado: `asignado_jornada`
5. Se quita de jornada → Estado: `creado`

### Escenarios de Error (Prevenidos)
- ❌ Intentar asignar combo `asignado_concursantes` a otro concursante
- ❌ Intentar asignar combo `asignado_jornada` a otra jornada  
- ❌ Cambiar manualmente estado de combos asignados

### Integración con Cuestionarios
Este sistema funciona de manera independiente pero complementaria al control de cuestionarios:
- **Cuestionarios**: Control similar con `asignado_jornada` y `asignado_concursantes`
- **Combos**: Control idéntico con los mismos estados y lógica
- **Consistencia**: Ambos sistemas usan la misma nomenclatura y comportamiento

## 📝 Notas Técnicas

### Backend
- **JornadaService**: Validaciones y cambios automáticos para jornadas
- **ConcursanteService**: Validaciones y cambios automáticos para concursantes  
- **ComboService**: Nuevo método `obtenerDisponiblesParaConcursantes()`
- **Transacciones**: Operaciones atómicas para evitar estados inconsistentes

### Frontend
- **utils.js**: Nuevas funciones `formatearEstadoCombo()` y badges
- **combos.js**: Badges informativos (sin edición manual para estados automáticos)
- **concursantes.js**: Badges en selección de combos
- **jornadas.js**: Visualización mejorada en selección y detalles
- **CSS**: Estilos específicos para estados de combos

---

**Sistema implementado exitosamente** ✅  
**Fecha**: 17 de julio de 2025  
**Versión**: 1.0.0 