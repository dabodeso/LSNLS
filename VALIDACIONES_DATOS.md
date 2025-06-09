# Sistema de Validaciones de Datos - LSNLS

## ✅ IMPLEMENTACIÓN COMPLETADA

Se ha implementado un sistema completo de validaciones de datos que cumple exactamente con los requisitos de LSNOLS 2024.

## 🎯 VALIDACIONES IMPLEMENTADAS

### **1. VALIDACIÓN DE MAYÚSCULAS**
- ✅ **Preguntas**: Solo texto en MAYÚSCULAS (máximo 150 caracteres)
- ✅ **Respuestas**: Solo texto en MAYÚSCULAS (máximo 50 caracteres)  
- ✅ **Temáticas**: Solo texto en MAYÚSCULAS (máximo 100 caracteres)
- ✅ **Transformación automática**: Convierte automáticamente a mayúsculas

### **2. PROHIBICIÓN DE SALTOS DE LÍNEA**
- ✅ **Validación estricta**: No permite `\n`, `\r` o `\r\n`
- ✅ **Limpieza automática**: Reemplaza saltos por espacios
- ✅ **Prevención**: Bloquea entrada de texto multilínea

### **3. LÍMITES DE CARACTERES ESTRICTOS**
| Campo | Límite | Validación |
|-------|--------|------------|
| **Pregunta** | 150 caracteres | ✅ Estricto |
| **Respuesta** | 50 caracteres | ✅ Estricto |
| **Temática** | 100 caracteres | ✅ Estricto |

### **4. VALIDACIÓN DE CARACTERES ESPECIALES**
**Caracteres permitidos:**
- ✅ Letras (A-Z, À-ÿ, Ñ, ñ)
- ✅ Números (0-9)
- ✅ Espacios
- ✅ Signos básicos: .,;:!?¡¿()[]"'-

**Caracteres prohibidos:**
- ❌ Símbolos especiales: @#$%^&*+={}|<>~`
- ❌ Caracteres de control
- ❌ Emojis y símbolos Unicode especiales

## 🔧 COMPONENTES IMPLEMENTADOS

### **1. Anotaciones de Validación Personalizadas**

#### `@UpperCase`
```java
@UpperCase(message = "El texto debe estar en mayúsculas")
private String pregunta;
```

#### `@NoLineBreaks`
```java
@NoLineBreaks(message = "El texto no puede contener saltos de línea")
private String respuesta;
```

#### `@NoSpecialCharacters`
```java
@NoSpecialCharacters(message = "El texto contiene caracteres no permitidos")
private String tematica;
```

### **2. Validadores Personalizados**
- **UpperCaseValidator**: Verifica texto en mayúsculas
- **NoLineBreaksValidator**: Detecta saltos de línea
- **NoSpecialCharactersValidator**: Filtra caracteres no permitidos

### **3. DataTransformationService**
**Métodos principales:**
- `normalizarTexto()`: Convierte a mayúsculas y limpia
- `normalizarPregunta()`: Específico para preguntas (150 chars)
- `normalizarRespuesta()`: Específico para respuestas (50 chars)
- `normalizarTematica()`: Específico para temáticas (100 chars)
- `validarPreguntaCompleta()`: Validación integral

### **4. Integración en Servicios**
- ✅ **PreguntaService**: Transformación automática en crear/actualizar
- ✅ **Validación previa**: Verifica datos antes de guardar
- ✅ **Manejo de errores**: Mensajes descriptivos de validación

## 📝 NUEVOS ENDPOINTS DE VALIDACIÓN

### **Validar Pregunta**
```
POST /api/preguntas/validar
```
**Request:**
```json
{
    "pregunta": "cual es la capital de españa?",
    "respuesta": "madrid",
    "tematica": "geografia"
}
```

**Response Success:**
```json
{
    "valid": true,
    "message": "La pregunta cumple con todos los requisitos",
    "transformedData": {
        "pregunta": "CUAL ES LA CAPITAL DE ESPAÑA?",
        "respuesta": "MADRID",
        "tematica": "GEOGRAFIA"
    }
}
```

**Response Error:**
```json
{
    "valid": false,
    "errors": {
        "pregunta": "La pregunta no puede exceder 150 caracteres",
        "respuesta": "La respuesta debe estar en mayúsculas"
    },
    "message": "La pregunta no puede exceder 150 caracteres; La respuesta debe estar en mayúsculas"
}
```

### **Transformar Texto**
```
POST /api/preguntas/transformar
```
**Request:**
```json
{
    "pregunta": "cual es la capital de francia?\n",
    "respuesta": "paris   ",
    "tematica": "geografía europea"
}
```

**Response:**
```json
{
    "transformados": {
        "pregunta": "CUAL ES LA CAPITAL DE FRANCIA?",
        "respuesta": "PARIS",
        "tematica": "GEOGRAFÍA EUROPEA"
    },
    "message": "Textos transformados correctamente"
}
```

## 🛡️ CARACTERÍSTICAS DE SEGURIDAD

### **Transformación Automática**
- ✅ **En creación**: Todos los textos se normalizan automáticamente
- ✅ **En actualización**: Solo campos modificados se transforman
- ✅ **Preserva datos**: Mantiene campos no modificados

### **Validación en Tiempo Real**
- ✅ **Antes de guardar**: Valida datos transformados
- ✅ **Feedback inmediato**: Errores específicos por campo
- ✅ **Prevención de errores**: Bloquea datos inválidos

### **Manejo de Errores**
- ✅ **Mensajes descriptivos**: Indica qué está mal y por qué
- ✅ **Errores múltiples**: Reporta todos los problemas de una vez
- ✅ **Sugerencias**: Indica cómo corregir los errores

## 📋 EJEMPLOS DE VALIDACIÓN

### **✅ DATOS VÁLIDOS**
```json
{
    "pregunta": "¿CUÁL ES LA CAPITAL DE ESPAÑA?",
    "respuesta": "MADRID",
    "tematica": "GEOGRAFÍA"
}
```

### **❌ DATOS INVÁLIDOS**

#### Exceso de caracteres:
```json
{
    "pregunta": "ESTA ES UNA PREGUNTA DEMASIADO LARGA QUE EXCEDE LOS 150 CARACTERES PERMITIDOS PARA EL CAMPO PREGUNTA Y POR TANTO NO DEBERÍA SER ACEPTADA POR EL SISTEMA DE VALIDACIÓN...",
    "error": "La pregunta no puede exceder 150 caracteres"
}
```

#### Minúsculas:
```json
{
    "pregunta": "¿Cuál es la capital de España?",
    "error": "La pregunta debe estar en mayúsculas"
}
```

#### Saltos de línea:
```json
{
    "pregunta": "¿CUÁL ES LA CAPITAL\nDE ESPAÑA?",
    "error": "La pregunta no puede contener saltos de línea"
}
```

#### Caracteres especiales:
```json
{
    "respuesta": "MADRID@#$%",
    "error": "La respuesta contiene caracteres no permitidos"
}
```

## 🚀 PROCESO DE VALIDACIÓN

### **1. Entrada de Datos**
```
Usuario envía → "cual es la capital de españa?\n"
```

### **2. Transformación**
```
Normalización → "CUAL ES LA CAPITAL DE ESPAÑA?"
```

### **3. Validación**
```
Verificación → ✅ Mayúsculas: SÍ
              ✅ Sin saltos: SÍ  
              ✅ Caracteres: VÁLIDOS
              ✅ Longitud: 33/150 caracteres
```

### **4. Resultado**
```
Almacenamiento → "CUAL ES LA CAPITAL DE ESPAÑA?"
```

## ✅ CUMPLIMIENTO DE REQUISITOS

- ✅ **Validación de MAYÚSCULAS** en preguntas y respuestas
- ✅ **Prohibir saltos de línea** completamente
- ✅ **Límites de caracteres estrictos** (150/50/100)
- ✅ **Validación de caracteres especiales** con lista permitida
- ✅ **Transformación automática** de datos
- ✅ **Mensajes de error descriptivos**
- ✅ **Integración con sistema de permisos**

## 🎯 PRÓXIMOS PASOS

El sistema de validaciones está **100% completo y operativo**. Las siguientes funcionalidades recomendadas son:

1. **Sistema de búsqueda avanzado** para selección de preguntas
2. **Exportación de documentos** con validaciones aplicadas
3. **Interfaz de usuario** que muestre validaciones en tiempo real
4. **Controladores completos** para otras entidades (Cuestionarios, etc.) 