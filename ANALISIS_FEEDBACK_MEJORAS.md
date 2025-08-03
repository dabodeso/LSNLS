# ANÁLISIS DE FEEDBACK - MEJORAS REQUERIDAS

## RESUMEN DEL FEEDBACK

El usuario ha proporcionado feedback detallado después de pruebas de usabilidad. Los cambios se dividen en categorías:

---

## 1. CAMBIOS GENERALES

### 1.1 Botón "Editar" en Acciones
**Descripción**: En las tablas de todas las secciones, además del botón "Borrar", añadir un botón "Editar" que abra el modal de creación/edición correspondiente.

**Estado Actual**: 
- Solo existe botón "Borrar" en acciones
- Edición inline mediante doble-click en celdas

**Cambios Necesarios**:
- Añadir botón "Editar" en columna "Acciones" de todas las tablas
- Implementar función para abrir modal pre-poblado con datos existentes
- Aplicar a: Preguntas, Concursantes, Cuestionarios, Combos, Jornadas

**Archivos Afectados**:
- `preguntas.html` y `preguntas.js`
- `concursantes.html` y `concursantes.js` 
- `cuestionarios.html` y `cuestionarios.js`
- `combos.html` y `combos.js`
- `jornadas.html` y `jornadas.js`

---

## 2. CAMBIOS EN TABLAS (PREGUNTAS Y CONCURSANTES)

### 2.1 Encabezados Fijos
**Descripción**: Implementar encabezados fijos para que al hacer scroll vertical, los encabezados de columna permanezcan visibles.

**Estado Actual**: 
- Encabezados desaparecen al hacer scroll
- Tablas con muchas filas pierden referencia de columnas

**Cambios Necesarios**:
- Implementar CSS `position: sticky` para `<thead>`
- Asegurar z-index apropiado
- Mantener alineación con columnas

**Archivos Afectados**:
- `styles.css` - nuevas reglas para `.table-preguntas` y `.tabla-concursantes-principal`
- Posiblemente ajustar estructura HTML si es necesario

---

## 3. CAMBIOS ESPECÍFICOS EN PREGUNTAS

### 3.1 Nuevos Estados de Pregunta
**Descripción**: Añadir nuevos estados para el flujo de corrección.

**Estados Actuales**: 
```java
enum EstadoPregunta {
    borrador, para_verificar, verificada, revisar, corregir, rechazada, aprobada
}
```

**Estados Requeridos**: Añadir "Para verificar" y "Para corregir" (ya existen `para_verificar` y `corregir`)

**Análisis**: Los estados solicitados YA EXISTEN en el enum. Verificar si están disponibles en frontend.

### 3.2 Filtro por Autoría
**Descripción**: Añadir filtro adicional por campo "Autoría" en la sección de filtros.

**Estado Actual**: 
- Filtros por: estado, nivel, temática, subtema
- Campo "Autoría" existe pero sin filtro

**Cambios Necesarios**:
- Añadir dropdown de filtro por autor en `preguntas.html`
- Implementar lógica de filtrado en `preguntas.js`
- Cargar lista de autores disponibles dinámicamente

---

## 4. CAMBIOS EN FORMULARIO "NUEVA PREGUNTA"

### 4.1 Reset de Formulario
**Descripción**: Al crear nueva pregunta, el formulario mantiene datos de la pregunta anterior.

**Problema**: Función `mostrarFormularioPregunta()` no limpia el formulario.

**Cambios Necesarios**:
- Añadir `document.getElementById('formCrearPregunta').reset()` en `mostrarFormularioPregunta()`

### 4.2 Campos de Texto Multilínea
**Descripción**: Los campos de texto largos deben mostrarse en múltiples líneas y ajustarse automáticamente.

**Campos Afectados**:
- Pregunta (principal)
- Respuesta  
- Datos Extra
- Fuentes
- Notas Verificación

**Cambios Necesarios**:
- Cambiar `<input type="text">` por `<textarea>` 
- Implementar auto-resize o altura fija apropiada
- Aplicar `white-space: pre-wrap` y `word-wrap: break-word`

### 4.3 Contador de Caracteres
**Descripción**: Campo "Pregunta" necesita contador de caracteres con límite de 150.

**Estado Actual**: 
- Campo pregunta sin contador
- Límite de 150 caracteres definido en backend

**Cambios Necesarios**:
- Añadir `<span>` contador al lado del campo
- JavaScript para actualizar contador en tiempo real
- Cambiar color a rojo cuando supere 150 caracteres
- Validación antes de submit

### 4.4 Hipervínculos Clickeables
**Descripción**: En campo "Fuentes", hacer los URLs clickeables.

**Cambios Necesarios**:
- Detectar URLs en el texto
- Convertir a enlaces `<a href="">` con `target="_blank"`
- Aplicar tanto en formulario como en vista de tabla

### 4.5 Campo Estado en Formulario
**Descripción**: Añadir selector de estado en el formulario de creación.

**Estado Actual**: 
- Estado se asigna automáticamente como "borrador"
- No hay control manual en creación

**Cambios Necesarios**:
- Añadir `<select>` con opciones: "Borrador", "Para verificar"
- Permitir al guionista elegir el estado inicial
- Validar que estados disponibles dependan del rol del usuario

---

## 5. CAMBIOS EN CUESTIONARIOS

### 5.1 Corrección de Estados
**Descripción**: Los estados de cuestionarios están incorrectos.

**Estados Actuales**:
```java
enum EstadoCuestionario {
    borrador, creado, adjudicado, grabado
}
```

**Estados Requeridos**:
- Borrador
- Revisar  
- Corregir
- Aprobado
- Adjudicado
- Grabado

**Cambios Necesarios**:
- Modificar enum en `Cuestionario.java`
- Actualizar `schema.sql` 
- Migrar datos existentes
- Actualizar frontend (dropdowns, filtros, badges)

---

## ANÁLISIS DE IMPACTO

### Base de Datos
- **CRÍTICO**: Cambio de enum en cuestionarios requiere migración de datos
- **MENOR**: Estados de preguntas ya existen

### Backend  
- **MEDIO**: Actualizar DTOs y servicios para nuevos estados
- **MENOR**: Lógica de autorización para estados de preguntas

### Frontend
- **ALTO**: Múltiples cambios en UI/UX
- **MEDIO**: Nuevos componentes (contadores, encabezados fijos)
- **BAJO**: Botones de edición (reutilizar lógica existente)

### Compatibilidad
- **Java 11**: Todos los cambios son compatibles
- **MySQL 5.7**: Compatible con cambios de enum

---

## PRIORIZACIÓN SUGERIDA

### PRIORIDAD 1 (Funcionalidad Crítica)
1. Corrección de estados de cuestionarios
2. Reset de formulario de preguntas
3. Botones de edición en tablas

### PRIORIDAD 2 (Usabilidad)
4. Encabezados fijos en tablas
5. Campo estado en formulario de pregunta
6. Filtro por autoría

### PRIORIDAD 3 (Mejoras de UX)
7. Contador de caracteres
8. Campos multilínea
9. Hipervínculos clickeables

---

## ESTIMACIÓN DE ESFUERZO

- **Total**: ~8-12 horas de desarrollo
- **Testing**: ~2-3 horas
- **Migración BD**: ~1 hora

Este análisis proporciona la base para implementar todos los cambios solicitados de manera sistemática. 