# 🔒 Configuración HTTPS para LSNLS

Este documento explica cómo configurar HTTPS gratuito para tu aplicación LSNLS.

## 📋 Opciones Disponibles

### 1. **Desarrollo Local (Certificado Autofirmado)**
- ✅ Gratuito
- ✅ Fácil de configurar
- ⚠️ El navegador mostrará advertencia de seguridad
- 🎯 **Recomendado para desarrollo**

### 2. **Producción (Let's Encrypt)**
- ✅ Completamente gratuito
- ✅ Certificado válido y confiable
- ✅ Renovación automática
- 🎯 **Recomendado para producción**

### 3. **Cloudflare (Proxy)**
- ✅ SSL/TLS gratuito
- ✅ CDN incluido
- ⚠️ Requiere dominio configurado
- 🎯 **Alternativa para producción**

## 🚀 Configuración Rápida - Desarrollo Local

### Windows
```bash
# 1. Generar certificado autofirmado
generate-self-signed-cert.bat

# 2. Iniciar aplicación con HTTPS
start-https.bat
```

### Linux/Mac
```bash
# 1. Dar permisos de ejecución
chmod +x *.sh

# 2. Generar certificado autofirmado
./generate-self-signed-cert.sh

# 3. Iniciar aplicación con HTTPS
./start-https.sh
```

### Acceso
- **URL**: https://localhost:8443
- **Usuario**: admin
- **Contraseña**: admin

## 🌐 Configuración para Producción - Let's Encrypt

### Prerrequisitos
1. **Dominio configurado** apuntando a tu servidor
2. **Puerto 80 libre** (para validación)
3. **Acceso de administrador** al servidor

### Windows
```bash
# 1. Instalar Certbot
# Descarga desde: https://certbot.eff.org/

# 2. Configurar HTTPS
setup-https.bat
```

### Linux/Mac
```bash
# 1. Instalar Certbot
sudo apt-get install certbot  # Ubuntu/Debian
brew install certbot          # macOS

# 2. Configurar HTTPS
./setup-https.sh
```

### Renovación Automática
```bash
# Agregar al crontab
sudo crontab -e

# Agregar esta línea:
0 12 * * * /usr/bin/certbot renew --quiet
```

## 🔧 Configuración Manual

### 1. Generar Certificado Autofirmado
```bash
# Crear directorio
mkdir -p ssl

# Generar certificado
openssl req -x509 -newkey rsa:4096 \
  -keyout ssl/key.pem \
  -out ssl/cert.pem \
  -days 365 -nodes \
  -subj "/C=ES/ST=Madrid/L=Madrid/O=LSNLS/OU=IT/CN=localhost"

# Convertir a PKCS12
openssl pkcs12 -export \
  -in ssl/cert.pem \
  -inkey ssl/key.pem \
  -out ssl/keystore.p12 \
  -name lsnls \
  -passout pass:lsnls2024
```

### 2. Configurar Spring Boot
Crear `src/main/resources/application-ssl.properties`:
```properties
# Habilitar SSL
server.ssl.enabled=true
server.ssl.key-store-type=PKCS12
server.ssl.key-store=classpath:ssl/keystore.p12
server.ssl.key-store-password=lsnls2024
server.ssl.key-alias=lsnls

# Puerto HTTPS
server.port=8443

# Habilitar HTTP/2
server.http2.enabled=true
```

### 3. Iniciar con HTTPS
```bash
mvn spring-boot:run -Dspring.profiles.active=ssl
```

## 🔍 Solución de Problemas

### Error: "Certificado no válido"
- **Causa**: Certificado autofirmado
- **Solución**: Haz clic en "Avanzado" → "Continuar a localhost"

### Error: "Puerto 80 en uso"
- **Causa**: Otro servicio usando puerto 80
- **Solución**: Detén el servicio o usa otro puerto

### Error: "Dominio no resuelve"
- **Causa**: DNS no configurado correctamente
- **Solución**: Verifica la configuración DNS

### Error: "Permisos denegados"
- **Causa**: Falta de permisos de administrador
- **Solución**: Ejecuta como administrador

## 📁 Estructura de Archivos

```
lsnls/
├── ssl/
│   ├── cert.pem          # Certificado público
│   ├── key.pem           # Clave privada
│   └── keystore.p12      # Formato Spring Boot
├── src/main/resources/
│   ├── ssl/
│   │   └── keystore.p12  # Certificado para la app
│   └── application-ssl.properties
├── generate-self-signed-cert.bat
├── generate-self-signed-cert.sh
├── setup-https.bat
├── setup-https.sh
├── start-https.bat
├── start-https.sh
└── HTTPS_SETUP.md
```

## 🔐 Seguridad

### Certificado Autofirmado
- ✅ Encripta la comunicación
- ⚠️ No verifica la identidad del servidor
- 🎯 **Adecuado para desarrollo**

### Let's Encrypt
- ✅ Encripta la comunicación
- ✅ Verifica la identidad del servidor
- ✅ Renovación automática
- 🎯 **Adecuado para producción**

## 📞 Soporte

Si tienes problemas:
1. Verifica que OpenSSL esté instalado
2. Asegúrate de tener permisos de administrador
3. Revisa los logs de la aplicación
4. Consulta la documentación de Let's Encrypt

## 🔄 Migración de HTTP a HTTPS

### 1. Actualizar URLs en el Frontend
```javascript
// Cambiar de:
const API_URL = 'http://localhost:8080/api';

// A:
const API_URL = 'https://localhost:8443/api';
```

### 2. Actualizar CORS
```java
@CrossOrigin(origins = "https://localhost:8443")
```

### 3. Configurar Redirección
```java
// Redirigir HTTP a HTTPS automáticamente
@Bean
public ServletWebServerFactory servletContainer() {
    TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
        @Override
        protected void postProcessContext(Context context) {
            SecurityConstraint securityConstraint = new SecurityConstraint();
            securityConstraint.setUserConstraint("CONFIDENTIAL");
            SecurityCollection collection = new SecurityCollection();
            collection.addPattern("/*");
            securityConstraint.addCollection(collection);
            context.addConstraint(securityConstraint);
        }
    };
    tomcat.addAdditionalTomcatConnectors(redirectConnector());
    return tomcat;
}
```

¡Tu aplicación LSNLS ahora está segura con HTTPS! 🔒 