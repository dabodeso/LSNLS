# Endpoints de Autenticación - LSNLS

## Autenticación implementada

El sistema ahora cuenta con autenticación JWT completa con los siguientes endpoints:

### 1. Registro de Usuario
**POST** `/api/auth/register`

```json
{
  "nombre": "usuario_prueba",
  "password": "contraseña123",
  "rol": "guion"
}
```

**Respuesta exitosa:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "id": 1,
  "nombre": "usuario_prueba",
  "rol": "guion"
}
```

### 2. Inicio de Sesión
**POST** `/api/auth/login`

```json
{
  "nombre": "admin",
  "password": "admin123"
}
```

**Respuesta exitosa:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "id": 1,
  "nombre": "admin",
  "rol": "direccion"
}
```

### 3. Cerrar Sesión
**POST** `/api/auth/logout`

Headers: `Authorization: Bearer {token}`

**Respuesta:**
```
"Sesión cerrada exitosamente"
```

### 4. Usuario Actual
**GET** `/api/auth/me`

Headers: `Authorization: Bearer {token}`

**Respuesta:**
```
"Usuario autenticado: admin"
```

## Uso del Token

Para acceder a endpoints protegidos, incluir el header:
```
Authorization: Bearer {token_obtenido_en_login}
```

## Roles de Usuario

- **consulta**: Solo lectura
- **guion**: Edición en determinados niveles
- **verificacion**: Edición en determinados niveles
- **direccion**: Edición en todos los niveles y capacidad de validación

## Usuarios de Prueba

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| admin | admin123 | direccion |
| guionista1 | admin123 | guion |
| verificador1 | admin123 | verificacion |
| consultor1 | admin123 | consulta |

## Configuración de Seguridad

- **Encriptación**: BCrypt para contraseñas
- **Tokens JWT**: Válidos por 24 horas
- **Sesiones**: Stateless (sin estado en servidor)
- **CORS**: Habilitado para desarrollo
- **Endpoints públicos**: `/api/auth/**` 