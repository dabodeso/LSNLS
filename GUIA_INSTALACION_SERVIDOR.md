# 🚀 **GUÍA DE INSTALACIÓN LSNLS EN SERVIDOR**

## 📋 **REQUISITOS DEL SISTEMA**

### **Versiones Mínimas:**
- **Java**: 17 o superior
- **MySQL**: 8.0 o superior
- **Maven**: 3.6 o superior
- **Sistema Operativo**: Linux (Ubuntu/Debian recomendado), Windows Server, o macOS

### **Requisitos de Hardware:**
- **RAM**: Mínimo 2GB, recomendado 4GB+
- **CPU**: 2 cores mínimo, 4+ recomendado
- **Disco**: 10GB mínimo para aplicación + base de datos
- **Red**: Puerto 8080 (HTTP) y 3306 (MySQL)

---

## 🔧 **PASO 1: INSTALAR JAVA 17**

### **Ubuntu/Debian:**
```bash
# Actualizar repositorios
sudo apt update

# Instalar Java 17
sudo apt install openjdk-17-jdk

# Verificar instalación
java -version
javac -version
```

### **CentOS/RHEL:**
```bash
# Instalar Java 17
sudo yum install java-17-openjdk-devel

# Verificar instalación
java -version
javac -version
```

### **Windows Server:**
1. Descargar OpenJDK 17 desde: https://adoptium.net/
2. Ejecutar el instalador
3. Configurar variables de entorno:
   ```cmd
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

---

## 🔧 **PASO 2: INSTALAR MAVEN**

### **Ubuntu/Debian:**
```bash
# Instalar Maven
sudo apt install maven

# Verificar instalación
mvn -version
```

### **CentOS/RHEL:**
```bash
# Instalar Maven
sudo yum install maven

# Verificar instalación
mvn -version
```

### **Windows Server:**
1. Descargar Maven desde: https://maven.apache.org/download.cgi
2. Extraer en `C:\Program Files\Apache\maven`
3. Configurar variables de entorno:
   ```cmd
   set MAVEN_HOME=C:\Program Files\Apache\maven
   set PATH=%MAVEN_HOME%\bin;%PATH%
   ```

---

## 🗄️ **PASO 3: INSTALAR MYSQL 8.0**

### **Ubuntu/Debian:**
```bash
# Actualizar repositorios
sudo apt update

# Instalar MySQL Server
sudo apt install mysql-server

# Iniciar y habilitar MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# Configurar seguridad
sudo mysql_secure_installation
```

### **CentOS/RHEL:**
```bash
# Instalar MySQL Server
sudo yum install mysql-server

# Iniciar y habilitar MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld

# Obtener contraseña temporal
sudo grep 'temporary password' /var/log/mysqld.log

# Configurar seguridad
sudo mysql_secure_installation
```

### **Windows Server:**
1. Descargar MySQL desde: https://dev.mysql.com/downloads/mysql/
2. Ejecutar el instalador
3. Seguir el asistente de configuración
4. Recordar la contraseña del root

---

## 🔧 **PASO 4: CONFIGURAR MYSQL**

### **Crear Base de Datos y Usuario:**
```sql
-- Conectar como root
mysql -u root -p

-- Crear base de datos
CREATE DATABASE lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Crear usuario para la aplicación
CREATE USER 'lsnls_user'@'localhost' IDENTIFIED BY 'tu_password_seguro';

-- Otorgar permisos
GRANT ALL PRIVILEGES ON lsnls.* TO 'lsnls_user'@'localhost';
FLUSH PRIVILEGES;

-- Verificar
SHOW DATABASES;
SELECT User, Host FROM mysql.user;
```

---

## 📁 **PASO 5: DESCARGAR Y CONFIGURAR LSNLS**

### **Clonar/Descargar el Repositorio:**
```bash
# Opción 1: Git (si tienes acceso)
git clone https://github.com/tu-usuario/LSNLS.git
cd LSNLS/lsnls

# Opción 2: Descargar ZIP y extraer
# Descargar desde GitHub y extraer en /opt/lsnls
cd /opt/lsnls/lsnls
```

### **Configurar Base de Datos:**
```bash
# Editar application.properties
nano src/main/resources/application.properties
```

**Configuración mínima:**
```properties
# Configuración de base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/lsnls?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=lsnls_user
spring.datasource.password=tu_password_seguro
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Configuración del servidor
server.port=8080
server.ssl.enabled=true
server.ssl.key-store=classpath:ssl/keystore.p12
server.ssl.key-store-password=tu_keystore_password
server.ssl.key-store-type=PKCS12

# Configuración de logging
logging.level.com.lsnls=INFO
logging.file.name=logs/lsnls.log
```

---

## 🔐 **PASO 6: CONFIGURAR SSL (OPCIONAL)**

### **Generar Certificado SSL:**
```bash
# Crear directorio para certificados
mkdir -p src/main/resources/ssl

# Generar keystore (para desarrollo)
keytool -genkeypair -alias lsnls -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/ssl/keystore.p12 \
  -validity 3650 -storepass tu_keystore_password
```

### **Para Producción (Recomendado):**
1. Obtener certificado SSL de Let's Encrypt o proveedor
2. Convertir a formato PKCS12
3. Colocar en `src/main/resources/ssl/keystore.p12`

---

## 🏗️ **PASO 7: COMPILAR Y EJECUTAR**

### **Compilar el Proyecto:**
```bash
# Limpiar y compilar
mvn clean package -DskipTests

# Verificar que se creó el JAR
ls -la target/lsnls-1.0-SNAPSHOT.jar
```

### **Ejecutar la Aplicación:**
```bash
# Opción 1: Ejecutar directamente
java -jar target/lsnls-1.0-SNAPSHOT.jar

# Opción 2: Con parámetros específicos
java -Xmx2g -Xms1g -jar target/lsnls-1.0-SNAPSHOT.jar \
  --spring.profiles.active=production
```

---

## 🔧 **PASO 8: CONFIGURAR COMO SERVICIO (RECOMENDADO)**

### **Crear Archivo de Servicio Systemd (Linux):**
```bash
sudo nano /etc/systemd/system/lsnls.service
```

**Contenido del archivo:**
```ini
[Unit]
Description=LSNLS Application
After=network.target mysql.service

[Service]
Type=simple
User=lsnls
WorkingDirectory=/opt/lsnls/lsnls
ExecStart=/usr/bin/java -Xmx2g -Xms1g -jar target/lsnls-1.0-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### **Habilitar y Iniciar el Servicio:**
```bash
# Crear usuario para la aplicación
sudo useradd -r -s /bin/false lsnls

# Asignar permisos
sudo chown -R lsnls:lsnls /opt/lsnls

# Habilitar y iniciar servicio
sudo systemctl daemon-reload
sudo systemctl enable lsnls
sudo systemctl start lsnls

# Verificar estado
sudo systemctl status lsnls
```

### **Para Windows Server:**
1. Usar NSSM (Non-Sucking Service Manager)
2. O crear un script batch y configurar como servicio de Windows

---

## 🔍 **PASO 9: VERIFICAR INSTALACIÓN**

### **Verificar que la Aplicación Funciona:**
```bash
# Verificar que el puerto está abierto
netstat -tlnp | grep 8080

# Verificar logs
tail -f logs/lsnls.log

# Probar conexión HTTP
curl http://localhost:8080/api/health
```

### **Verificar Base de Datos:**
```bash
# Conectar a MySQL
mysql -u lsnls_user -p lsnls

# Verificar tablas creadas
SHOW TABLES;

# Verificar datos iniciales
SELECT COUNT(*) FROM preguntas;
SELECT COUNT(*) FROM usuarios;
```

---

## 🔧 **PASO 10: CONFIGURACIÓN ADICIONAL**

### **Configurar Firewall:**
```bash
# Ubuntu/Debian
sudo ufw allow 8080/tcp
sudo ufw allow 3306/tcp

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

### **Configurar Backup Automático:**
```bash
# Crear script de backup
sudo nano /opt/lsnls/backup.sh
```

**Contenido del script:**
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/lsnls/backups"
DB_NAME="lsnls"
DB_USER="lsnls_user"
DB_PASS="tu_password_seguro"

# Crear directorio de backup
mkdir -p $BACKUP_DIR

# Backup de base de datos
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/lsnls_db_$DATE.sql

# Backup de archivos de la aplicación
tar -czf $BACKUP_DIR/lsnls_app_$DATE.tar.gz /opt/lsnls/lsnls

# Mantener solo los últimos 7 backups
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
```

**Configurar cron para backup diario:**
```bash
# Editar crontab
crontab -e

# Agregar línea para backup diario a las 2 AM
0 2 * * * /opt/lsnls/backup.sh
```

---

## 🚨 **SOLUCIÓN DE PROBLEMAS**

### **Problemas Comunes:**

#### **1. Error de Conexión a Base de Datos:**
```bash
# Verificar que MySQL está ejecutándose
sudo systemctl status mysql

# Verificar conexión
mysql -u lsnls_user -p lsnls

# Verificar configuración en application.properties
```

#### **2. Error de Puerto en Uso:**
```bash
# Verificar qué está usando el puerto 8080
sudo netstat -tlnp | grep 8080

# Cambiar puerto en application.properties si es necesario
server.port=8081
```

#### **3. Error de Memoria:**
```bash
# Aumentar memoria JVM
java -Xmx4g -Xms2g -jar target/lsnls-1.0-SNAPSHOT.jar
```

#### **4. Error de SSL:**
```bash
# Verificar que el keystore existe
ls -la src/main/resources/ssl/

# Verificar contraseña del keystore
keytool -list -keystore src/main/resources/ssl/keystore.p12
```

---

## 📞 **CONTACTO Y SOPORTE**

### **Logs Importantes:**
- **Aplicación**: `/opt/lsnls/lsnls/logs/lsnls.log`
- **Sistema**: `sudo journalctl -u lsnls`
- **MySQL**: `/var/log/mysql/error.log`

### **Comandos Útiles:**
```bash
# Reiniciar aplicación
sudo systemctl restart lsnls

# Ver logs en tiempo real
sudo journalctl -u lsnls -f

# Verificar estado de servicios
sudo systemctl status lsnls mysql

# Backup manual
/opt/lsnls/backup.sh
```

---

## ✅ **CHECKLIST DE INSTALACIÓN**

- [ ] Java 17 instalado y configurado
- [ ] Maven instalado y configurado
- [ ] MySQL 8.0 instalado y configurado
- [ ] Base de datos `lsnls` creada
- [ ] Usuario `lsnls_user` creado con permisos
- [ ] Repositorio LSNLS descargado
- [ ] `application.properties` configurado
- [ ] Certificado SSL configurado (opcional)
- [ ] Aplicación compilada con Maven
- [ ] Aplicación ejecutándose en puerto 8080
- [ ] Servicio systemd configurado (opcional)
- [ ] Firewall configurado
- [ ] Backup automático configurado
- [ ] Aplicación accesible vía HTTP

**¡LSNLS está listo para usar en producción! 🎉** 