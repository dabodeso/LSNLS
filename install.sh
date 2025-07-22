#!/bin/bash

# 🚀 **SCRIPT DE INSTALACIÓN AUTOMATIZADA LSNLS**
# Este script instala y configura LSNLS en un servidor Linux

set -e  # Salir si hay algún error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para verificar si un comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Función para detectar el sistema operativo
detect_os() {
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        OS=$NAME
        VER=$VERSION_ID
    else
        print_error "No se pudo detectar el sistema operativo"
        exit 1
    fi
}

# Función para verificar si se ejecuta como root
check_root() {
    if [[ $EUID -eq 0 ]]; then
        print_warning "Este script no debe ejecutarse como root"
        print_warning "Se solicitarán permisos sudo cuando sea necesario"
        exit 1
    fi
}

# Función para actualizar el sistema
update_system() {
    print_status "Actualizando el sistema..."
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt update && sudo apt upgrade -y
    elif [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum update -y
    else
        print_warning "Sistema operativo no soportado: $OS"
        exit 1
    fi
    
    print_success "Sistema actualizado"
}

# Función para instalar Java 17
install_java() {
    print_status "Instalando Java 17..."
    
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ $JAVA_VERSION -ge 17 ]]; then
            print_success "Java $JAVA_VERSION ya está instalado"
            return
        fi
    fi
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt install -y openjdk-17-jdk
    elif [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y java-17-openjdk-devel
    fi
    
    print_success "Java 17 instalado"
}

# Función para instalar Maven
install_maven() {
    print_status "Instalando Maven..."
    
    if command_exists mvn; then
        print_success "Maven ya está instalado"
        return
    fi
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt install -y maven
    elif [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y maven
    fi
    
    print_success "Maven instalado"
}

# Función para instalar MySQL
install_mysql() {
    print_status "Instalando MySQL 8.0..."
    
    if command_exists mysql; then
        print_success "MySQL ya está instalado"
        return
    fi
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo apt install -y mysql-server
        sudo systemctl start mysql
        sudo systemctl enable mysql
    elif [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        sudo yum install -y mysql-server
        sudo systemctl start mysqld
        sudo systemctl enable mysqld
    fi
    
    print_success "MySQL instalado"
}

# Función para configurar MySQL
configure_mysql() {
    print_status "Configurando MySQL..."
    
    # Crear archivo de configuración temporal
    cat > /tmp/mysql_setup.sql << EOF
CREATE DATABASE IF NOT EXISTS lsnls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lsnls_user'@'localhost' IDENTIFIED BY 'lsnls_password_2024';
GRANT ALL PRIVILEGES ON lsnls.* TO 'lsnls_user'@'localhost';
FLUSH PRIVILEGES;
EOF
    
    # Ejecutar configuración
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        sudo mysql < /tmp/mysql_setup.sql
    elif [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        # Obtener contraseña temporal
        TEMP_PASSWORD=$(sudo grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
        if [[ -n "$TEMP_PASSWORD" ]]; then
            mysql -u root -p"$TEMP_PASSWORD" --connect-expired-password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root_password_2024';"
            mysql -u root -p'root_password_2024' < /tmp/mysql_setup.sql
        else
            mysql -u root < /tmp/mysql_setup.sql
        fi
    fi
    
    rm /tmp/mysql_setup.sql
    print_success "MySQL configurado"
}

# Función para crear directorio de la aplicación
create_app_directory() {
    print_status "Creando directorio de la aplicación..."
    
    sudo mkdir -p /opt/lsnls
    sudo chown $USER:$USER /opt/lsnls
    
    print_success "Directorio creado: /opt/lsnls"
}

# Función para generar certificado SSL
generate_ssl_certificate() {
    print_status "Generando certificado SSL..."
    
    mkdir -p src/main/resources/ssl
    
    # Generar keystore
    keytool -genkeypair -alias lsnls -keyalg RSA -keysize 2048 \
        -storetype PKCS12 -keystore src/main/resources/ssl/keystore.p12 \
        -validity 3650 -storepass lsnls_ssl_2024 -keypass lsnls_ssl_2024 \
        -dname "CN=localhost, OU=LSNLS, O=Development, L=City, S=State, C=ES" \
        -noprompt
    
    print_success "Certificado SSL generado"
}

# Función para configurar firewall
configure_firewall() {
    print_status "Configurando firewall..."
    
    if [[ "$OS" == *"Ubuntu"* ]] || [[ "$OS" == *"Debian"* ]]; then
        if command_exists ufw; then
            sudo ufw allow 8080/tcp
            sudo ufw allow 3306/tcp
            print_success "Firewall UFW configurado"
        fi
    elif [[ "$OS" == *"CentOS"* ]] || [[ "$OS" == *"Red Hat"* ]]; then
        if command_exists firewall-cmd; then
            sudo firewall-cmd --permanent --add-port=8080/tcp
            sudo firewall-cmd --permanent --add-port=3306/tcp
            sudo firewall-cmd --reload
            print_success "Firewall firewalld configurado"
        fi
    fi
}

# Función para crear script de backup
create_backup_script() {
    print_status "Creando script de backup..."
    
    cat > /opt/lsnls/backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/lsnls/backups"
DB_NAME="lsnls"
DB_USER="lsnls_user"
DB_PASS="lsnls_password_2024"

# Crear directorio de backup
mkdir -p $BACKUP_DIR

# Backup de base de datos
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/lsnls_db_$DATE.sql

# Backup de archivos de la aplicación
tar -czf $BACKUP_DIR/lsnls_app_$DATE.tar.gz /opt/lsnls/lsnls

# Mantener solo los últimos 7 backups
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete

echo "Backup completado: $DATE"
EOF
    
    chmod +x /opt/lsnls/backup.sh
    print_success "Script de backup creado"
}

# Función para crear servicio systemd
create_systemd_service() {
    print_status "Creando servicio systemd..."
    
    # Crear usuario para el servicio
    sudo useradd -r -s /bin/false lsnls 2>/dev/null || true
    
    # Crear archivo de servicio
    sudo tee /etc/systemd/system/lsnls.service > /dev/null << EOF
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
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    
    # Asignar permisos
    sudo chown -R lsnls:lsnls /opt/lsnls
    
    # Recargar systemd
    sudo systemctl daemon-reload
    
    print_success "Servicio systemd creado"
}

# Función para mostrar información final
show_final_info() {
    echo
    echo "=========================================="
    echo "🎉 INSTALACIÓN COMPLETADA"
    echo "=========================================="
    echo
    echo "📋 INFORMACIÓN IMPORTANTE:"
    echo "• Base de datos: lsnls"
    echo "• Usuario BD: lsnls_user"
    echo "• Contraseña BD: lsnls_password_2024"
    echo "• Puerto aplicación: 8080"
    echo "• Puerto MySQL: 3306"
    echo
    echo "🔧 PRÓXIMOS PASOS:"
    echo "1. Copiar el código de LSNLS a /opt/lsnls/lsnls"
    echo "2. Configurar application.properties"
    echo "3. Compilar: mvn clean package"
    echo "4. Iniciar: sudo systemctl start lsnls"
    echo
    echo "📁 DIRECTORIOS:"
    echo "• Aplicación: /opt/lsnls/lsnls"
    echo "• Logs: /opt/lsnls/lsnls/logs"
    echo "• Backups: /opt/lsnls/backups"
    echo
    echo "🔗 ACCESO:"
    echo "• Web: https://localhost:8080"
    echo "• API: https://localhost:8080/api"
    echo
    echo "⚠️  IMPORTANTE: Cambiar las contraseñas por defecto"
    echo "=========================================="
}

# Función principal
main() {
    echo "🚀 Iniciando instalación de LSNLS..."
    echo
    
    # Verificaciones iniciales
    check_root
    detect_os
    print_status "Sistema detectado: $OS $VER"
    
    # Instalación
    update_system
    install_java
    install_maven
    install_mysql
    configure_mysql
    create_app_directory
    configure_firewall
    create_backup_script
    create_systemd_service
    
    # Información final
    show_final_info
}

# Ejecutar función principal
main "$@" 