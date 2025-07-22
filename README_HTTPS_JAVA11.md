# 🌐 LSNLS - Configuración HTTPS con Java 11

## 📋 Requisitos

- **Java 11** (obligatorio)
- **Maven 3.6+**
- **MySQL 8.0+**
- **Base de datos `lsnls` creada**

## 🚀 Instalación Rápida

### 1. Configuración Automática
```bash
# Ejecutar script de configuración completa
setup-server-https.bat
```

### 2. Configuración Manual

#### Paso 1: Generar Certificado SSL
```bash
generate-ssl-java11.bat
```

#### Paso 2: Compilar y Empaquetar
```bash
mvn clean package -DskipTests
```

#### Paso 3: Ejecutar
```bash
java -jar target/lsnls-1.0-SNAPSHOT.jar
```

## 🔒 Configuración HTTPS

### Certificado SSL
- **Tipo**: PKCS12 (compatible con Java 11)
- **Algoritmo**: RSA 2048 bits
- **Protocolos**: TLSv1.2, TLSv1.3
- **Ubicación**: `src/main/resources/ssl/keystore.p12`
- **Contraseña**: `lsnls2024`

### Configuración del Servidor
- **Puerto**: 8080
- **Protocolo**: HTTPS
- **URL**: https://localhost:8080
- **CORS**: Habilitado para HTTP y HTTPS

## 🌐 Acceso a la Aplicación

### Desarrollo Local
```
https://localhost:8080
```

### Servidor de Producción
```
https://[IP-DEL-SERVIDOR]:8080
```

## ⚠️ Advertencia de Seguridad

El certificado es **autofirmado**, por lo que el navegador mostrará una advertencia:

1. Haz clic en **"Avanzado"**
2. Haz clic en **"Continuar a localhost (no seguro)"**

## 🔧 Scripts Disponibles

| Script | Descripción |
|--------|-------------|
| `generate-ssl-java11.bat` | Genera certificado SSL compatible con Java 11 |
| `start-https-java11.bat` | Inicia la aplicación con HTTPS |
| `setup-server-https.bat` | Configuración completa automática |

## 🛠️ Solución de Problemas

### Error: "Algorithm HmacPBESHA256 not available"
**Causa**: Certificado generado con Java más nuevo
**Solución**: Regenerar con `generate-ssl-java11.bat`

### Error: "Unknown database 'lsnls'"
**Causa**: Base de datos no existe
**Solución**: 
```sql
CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Error: "Port 8080 already in use"
**Causa**: Puerto ocupado
**Solución**: Cambiar puerto en `application.properties`

## 📁 Estructura de Archivos

```
lsnls/
├── src/main/resources/
│   ├── ssl/
│   │   └── keystore.p12          # Certificado SSL
│   └── application.properties    # Configuración
├── target/
│   └── lsnls-1.0-SNAPSHOT.jar   # Aplicación compilada
├── generate-ssl-java11.bat       # Generar SSL
├── start-https-java11.bat        # Iniciar aplicación
└── setup-server-https.bat        # Configuración completa
```

## 🔐 Seguridad

### Certificado de Producción
Para producción, reemplaza el certificado autofirmado con:
- **Let's Encrypt** (gratuito)
- **Certificado comercial** (de pago)

### Configuración de Firewall
```bash
# Abrir puerto 8080 para HTTPS
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload
```

## 📞 Soporte

Si tienes problemas:
1. Verifica que Java 11 esté instalado: `java -version`
2. Verifica que MySQL esté ejecutándose
3. Verifica que la base de datos `lsnls` exista
4. Revisa los logs de la aplicación 