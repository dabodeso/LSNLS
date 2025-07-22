# 🚀 Configuración HTTPS para Producción - LSNLS

## 📋 Prerrequisitos

### 1. **IP Pública Estable**
- Tu servidor debe tener una IP pública fija o estable
- La IP debe ser accesible desde internet
- Ejemplo: `203.0.113.10`

### 2. **Puertos Abiertos**
- **Puerto 80**: Para verificación de Let's Encrypt
- **Puerto 443**: Para HTTPS (producción)
- **Puerto 3306**: Para MySQL (si es remoto)

### 3. **Software Requerido**
- **Certbot**: Para obtener certificados Let's Encrypt
- **OpenSSL**: Para convertir certificados
- **Java 17+**: Para ejecutar la aplicación

## 🔧 Instalación de Certbot

### Opción 1: Descarga Directa
1. Ve a https://certbot.eff.org/
2. Selecciona tu sistema operativo
3. Sigue las instrucciones de instalación

### Opción 2: Chocolatey (Windows)
```bash
choco install certbot
```

### Opción 3: pip (Python)
```bash
pip install certbot
```

## 🛠️ Configuración Paso a Paso

### Paso 1: Preparar el Servidor
```bash
# Verificar que el puerto 80 esté libre
netstat -an | findstr :80

# Verificar que el puerto 443 esté libre
netstat -an | findstr :443
```

### Paso 2: Ejecutar Script de Configuración
```bash
# Ejecutar el script de configuración
setup-production-https.bat
```

El script te pedirá:
- Tu IP pública
- Verificará Certbot
- Obtendrá el certificado SSL
- Configurará Spring Boot

### Paso 3: Iniciar en Producción
```bash
# Iniciar la aplicación en modo producción
start-production.bat
```

## 🌐 Configuración de Red

### Firewall (Windows)
1. Abrir **Firewall de Windows Defender**
2. **Reglas de entrada** → **Nueva regla**
3. **Puerto** → **TCP** → **443**
4. **Permitir la conexión**
5. **Aplicar a todos los perfiles**

### Router (Port Forwarding)
Si usas un router, configura port forwarding:
- **Puerto externo**: 443
- **Puerto interno**: 443
- **IP interna**: Tu IP local del servidor
- **Protocolo**: TCP

## 🔒 Seguridad

### Configuraciones Aplicadas
- ✅ HTTPS obligatorio
- ✅ Cookies seguras
- ✅ Headers de seguridad
- ✅ TLS 1.2/1.3
- ✅ Logging reducido
- ✅ CORS configurado

### Recomendaciones Adicionales
1. **Cambiar contraseñas por defecto**
2. **Configurar backup automático**
3. **Monitorear logs**
4. **Actualizar regularmente**

## 🔄 Renovación Automática

### Configurar Tarea Programada
1. Abrir **Programador de tareas**
2. **Crear tarea básica**
3. **Nombre**: "Renovar Certificado LSNLS"
4. **Frecuencia**: Cada 60 días
5. **Acción**: Ejecutar programa
6. **Programa**: `certbot`
7. **Argumentos**: `renew --quiet`

### Verificar Renovación
```bash
# Verificar estado del certificado
certbot certificates

# Renovar manualmente
certbot renew
```

## 🚨 Solución de Problemas

### Error: "No se pudo obtener certificado"
**Causas posibles:**
- Puerto 80 bloqueado
- IP no accesible desde internet
- Firewall activo
- Router sin port forwarding

**Soluciones:**
1. Verificar puertos abiertos
2. Configurar port forwarding
3. Desactivar firewall temporalmente
4. Usar certificado autofirmado para pruebas

### Error: "Certificado no válido"
**Causas posibles:**
- Certificado expirado
- IP pública cambiada
- Problema con Let's Encrypt

**Soluciones:**
1. Renovar certificado: `certbot renew`
2. Obtener nuevo certificado: `setup-production-https.bat`
3. Verificar fecha del sistema

### Error: "Conexión rechazada"
**Causas posibles:**
- Puerto 443 bloqueado
- Aplicación no iniciada
- Firewall activo

**Soluciones:**
1. Verificar que la aplicación esté corriendo
2. Abrir puerto 443 en firewall
3. Configurar port forwarding

## 📊 Monitoreo

### Verificar Estado
```bash
# Verificar puertos
netstat -an | findstr :443

# Verificar certificado
certbot certificates

# Verificar logs
tail -f logs/lsnls.log
```

### Métricas Importantes
- **Uptime**: Tiempo de actividad
- **Response time**: Tiempo de respuesta
- **Error rate**: Tasa de errores
- **SSL certificate**: Estado del certificado

## 🔧 Comandos Útiles

### Desarrollo
```bash
# Iniciar en desarrollo
mvn spring-boot:run

# Iniciar con HTTPS local
start-https.bat
```

### Producción
```bash
# Configurar HTTPS
setup-production-https.bat

# Iniciar en producción
start-production.bat

# Crear JAR
mvn clean package

# Ejecutar JAR
java -jar target/lsnls-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Mantenimiento
```bash
# Renovar certificado
certbot renew

# Verificar certificados
certbot certificates

# Limpiar certificados expirados
certbot delete --cert-name TU_IP_PUBLICA
```

## 📞 Soporte

### Logs Importantes
- **Aplicación**: `logs/lsnls.log`
- **Certbot**: `C:\Certbot\logs\`
- **Spring Boot**: Consola de la aplicación

### Información Útil
- **IP Pública**: Usar servicios como whatismyip.com
- **Puertos**: Usar netstat para verificar
- **Certificados**: Usar certbot certificates

---

## ✅ Checklist de Producción

- [ ] IP pública configurada
- [ ] Puertos 80 y 443 abiertos
- [ ] Certbot instalado
- [ ] Certificado SSL obtenido
- [ ] Firewall configurado
- [ ] Port forwarding configurado
- [ ] Aplicación iniciada en modo producción
- [ ] Renovación automática configurada
- [ ] Backup configurado
- [ ] Monitoreo activo

**¡Tu aplicación LSNLS está lista para producción con HTTPS!** 🎉 