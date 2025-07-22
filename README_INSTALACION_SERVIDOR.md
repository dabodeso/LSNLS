# 🚀 **INSTALACIÓN RÁPIDA LSNLS EN SERVIDOR**

## 📋 **RESUMEN RÁPIDO**

Para instalar LSNLS en tu servidor, necesitas:

### **Requisitos Mínimos:**
- **Sistema**: Linux (Ubuntu/Debian/CentOS)
- **RAM**: 2GB mínimo, 4GB recomendado
- **Disco**: 10GB mínimo
- **Puertos**: 8080 (HTTPS), 3306 (MySQL)

### **Dependencias:**
- Java 17+
- Maven 3.6+
- MySQL 8.0+

---

## ⚡ **INSTALACIÓN AUTOMÁTICA (RECOMENDADA)**

### **Paso 1: Descargar el código**
```bash
# Clonar o descargar el repositorio
git clone https://github.com/tu-usuario/LSNLS.git
cd LSNLS/lsnls
```

### **Paso 2: Ejecutar instalación automática**
```bash
# Hacer ejecutable el script
chmod +x install.sh

# Ejecutar instalación
./install.sh
```

### **Paso 3: Copiar el código y configurar**
```bash
# Copiar código a directorio de aplicación
sudo cp -r . /opt/lsnls/lsnls/

# Configurar permisos
sudo chown -R lsnls:lsnls /opt/lsnls
```

### **Paso 4: Desplegar aplicación**
```bash
# Ir al directorio de la aplicación
cd /opt/lsnls/lsnls

# Hacer ejecutable el script de despliegue
chmod +x deploy.sh

# Desplegar (modo interactivo)
./deploy.sh

# O desplegar directamente
./deploy.sh full
```

---

## 🔧 **INSTALACIÓN MANUAL**

### **1. Instalar Java 17**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel
```

### **2. Instalar Maven**
```bash
# Ubuntu/Debian
sudo apt install maven

# CentOS/RHEL
sudo yum install maven
```

### **3. Instalar MySQL**
```bash
# Ubuntu/Debian
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# CentOS/RHEL
sudo yum install mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

### **4. Configurar MySQL**
```sql
mysql -u root -p

CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'lsnls_user'@'localhost' IDENTIFIED BY 'lsnls_password_2024';
GRANT ALL PRIVILEGES ON lsnls.* TO 'lsnls_user'@'localhost';
FLUSH PRIVILEGES;
```

### **5. Configurar aplicación**
```bash
# Copiar configuración de producción
cp application-prod.properties src/main/resources/application.properties

# Generar certificado SSL
mkdir -p src/main/resources/ssl
keytool -genkeypair -alias lsnls -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/ssl/keystore.p12 \
  -validity 3650 -storepass lsnls_ssl_2024 -keypass lsnls_ssl_2024 \
  -dname "CN=localhost, OU=LSNLS, O=Development, L=City, S=State, C=ES" \
  -noprompt
```

### **6. Compilar y ejecutar**
```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/lsnls-1.0-SNAPSHOT.jar
```

---

## 🔐 **CONFIGURACIÓN DE SEGURIDAD**

### **Cambiar contraseñas por defecto:**
```sql
-- Cambiar contraseña de usuario de aplicación
ALTER USER 'lsnls_user'@'localhost' IDENTIFIED BY 'tu_nueva_contraseña_segura';

-- Cambiar contraseña de root (opcional)
ALTER USER 'root'@'localhost' IDENTIFIED BY 'tu_nueva_contraseña_root';
```

### **Actualizar application.properties:**
```properties
# Cambiar contraseñas en el archivo
spring.datasource.password=tu_nueva_contraseña_segura
jwt.secret=tu_nuevo_jwt_secret_muy_largo_y_seguro
server.ssl.key-store-password=tu_nueva_contraseña_ssl
```

---

## 📊 **COMANDOS ÚTILES**

### **Gestión del servicio:**
```bash
# Estado del servicio
sudo systemctl status lsnls

# Iniciar servicio
sudo systemctl start lsnls

# Detener servicio
sudo systemctl stop lsnls

# Reiniciar servicio
sudo systemctl restart lsnls

# Habilitar inicio automático
sudo systemctl enable lsnls
```

### **Logs y monitoreo:**
```bash
# Ver logs en tiempo real
sudo journalctl -u lsnls -f

# Ver logs de la aplicación
tail -f /opt/lsnls/lsnls/logs/lsnls.log

# Ver logs de MySQL
sudo tail -f /var/log/mysql/error.log
```

### **Backup:**
```bash
# Backup manual
/opt/lsnls/backup.sh

# Verificar backups
ls -la /opt/lsnls/backups/
```

---

## 🔍 **VERIFICACIÓN DE INSTALACIÓN**

### **Verificar servicios:**
```bash
# Verificar que MySQL está ejecutándose
sudo systemctl status mysql

# Verificar que LSNLS está ejecutándose
sudo systemctl status lsnls

# Verificar puertos abiertos
netstat -tlnp | grep -E "(8080|3306)"
```

### **Configurar firewall:**
```bash
# Ubuntu/Debian
sudo ufw allow 8080/tcp
sudo ufw allow 3306/tcp

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

### **Verificar aplicación:**
```bash
# Probar conexión HTTPS
curl -k https://localhost:8080

# Verificar base de datos
mysql -u lsnls_user -p lsnls -e "SHOW TABLES;"
```

---

## 🚨 **SOLUCIÓN DE PROBLEMAS**

### **Error: Puerto 8080 en uso**
```bash
# Ver qué está usando el puerto
sudo netstat -tlnp | grep 8080

# Cambiar puerto en application.properties
server.port=8081
```

### **Error: Conexión a base de datos**
```bash
# Verificar que MySQL está ejecutándose
sudo systemctl status mysql

# Verificar credenciales
mysql -u lsnls_user -p lsnls

# Verificar configuración
cat src/main/resources/application.properties | grep datasource
```

### **Error: Memoria insuficiente**
```bash
# Aumentar memoria JVM en el servicio
sudo nano /etc/systemd/system/lsnls.service

# Cambiar la línea ExecStart:
ExecStart=/usr/bin/java -Xmx4g -Xms2g -jar target/lsnls-1.0-SNAPSHOT.jar

# Recargar y reiniciar
sudo systemctl daemon-reload
sudo systemctl restart lsnls
```

### **Error: Certificado SSL**
```bash
# Verificar que el keystore existe
ls -la src/main/resources/ssl/

# Regenerar certificado
rm src/main/resources/ssl/keystore.p12
./deploy.sh full
```

---

## 📞 **SOPORTE**

### **Logs importantes:**
- **Aplicación**: `/opt/lsnls/lsnls/logs/lsnls.log`
- **Sistema**: `sudo journalctl -u lsnls`
- **MySQL**: `/var/log/mysql/error.log`

### **Información del sistema:**
```bash
# Versión de Java
java -version

# Versión de Maven
mvn -version

# Versión de MySQL
mysql --version

# Espacio en disco
df -h

# Memoria disponible
free -h
```

---

## ✅ **CHECKLIST FINAL**

- [ ] Java 17 instalado y funcionando
- [ ] Maven instalado y funcionando
- [ ] MySQL instalado y ejecutándose
- [ ] Base de datos `lsnls` creada
- [ ] Usuario `lsnls_user` creado con permisos
- [ ] Código copiado a `/opt/lsnls/lsnls`
- [ ] `application.properties` configurado
- [ ] Certificado SSL generado
- [ ] Aplicación compilada exitosamente
- [ ] Servicio systemd configurado y ejecutándose
- [ ] Puerto 8080 abierto y accesible
- [ ] Aplicación responde en https://localhost:8080
- [ ] Contraseñas por defecto cambiadas
- [ ] Backup automático configurado

**¡LSNLS está listo para usar en producción! 🎉**

---

## 🔗 **ENLACES ÚTILES**

- [Guía de instalación completa](GUIA_INSTALACION_SERVIDOR.md)
- [Configuración de producción](application-prod.properties)
- [Script de instalación automática](install.sh)
- [Script de despliegue](deploy.sh) 