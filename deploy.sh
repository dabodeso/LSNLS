#!/bin/bash

# 🚀 **SCRIPT DE DESPLIEGUE RÁPIDO LSNLS**
# Este script compila y despliega LSNLS en el servidor

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

# Variables de configuración
APP_DIR="/opt/lsnls/lsnls"
JAR_NAME="lsnls-1.0-SNAPSHOT.jar"
SERVICE_NAME="lsnls"

# Función para verificar dependencias
check_dependencies() {
    print_status "Verificando dependencias..."
    
    if ! command -v java &> /dev/null; then
        print_error "Java no está instalado"
        exit 1
    fi
    
    if ! command -v mvn &> /dev/null; then
        print_error "Maven no está instalado"
        exit 1
    fi
    
    if ! command -v mysql &> /dev/null; then
        print_error "MySQL no está instalado"
        exit 1
    fi
    
    print_success "Todas las dependencias están instaladas"
}

# Función para verificar directorio de la aplicación
check_app_directory() {
    if [[ ! -d "$APP_DIR" ]]; then
        print_error "Directorio de aplicación no encontrado: $APP_DIR"
        print_error "Ejecuta primero el script de instalación: ./install.sh"
        exit 1
    fi
    
    cd "$APP_DIR"
    print_success "Directorio de aplicación verificado"
}

# Función para generar certificado SSL si no existe
generate_ssl_if_needed() {
    if [[ ! -f "src/main/resources/ssl/keystore.p12" ]]; then
        print_status "Generando certificado SSL..."
        
        mkdir -p src/main/resources/ssl
        
        keytool -genkeypair -alias lsnls -keyalg RSA -keysize 2048 \
            -storetype PKCS12 -keystore src/main/resources/ssl/keystore.p12 \
            -validity 3650 -storepass lsnls_ssl_2024 -keypass lsnls_ssl_2024 \
            -dname "CN=localhost, OU=LSNLS, O=Development, L=City, S=State, C=ES" \
            -noprompt
        
        print_success "Certificado SSL generado"
    else
        print_success "Certificado SSL ya existe"
    fi
}

# Función para compilar la aplicación
compile_application() {
    print_status "Compilando aplicación..."
    
    # Limpiar compilación anterior
    mvn clean
    
    # Compilar sin tests para producción
    mvn package -DskipTests
    
    if [[ ! -f "target/$JAR_NAME" ]]; then
        print_error "Error al compilar la aplicación"
        exit 1
    fi
    
    print_success "Aplicación compilada exitosamente"
}

# Función para verificar base de datos
check_database() {
    print_status "Verificando conexión a base de datos..."
    
    if mysql -u lsnls_user -plsnls_password_2024 -e "USE lsnls;" 2>/dev/null; then
        print_success "Conexión a base de datos exitosa"
    else
        print_error "Error al conectar a la base de datos"
        print_error "Verifica que MySQL esté ejecutándose y las credenciales sean correctas"
        exit 1
    fi
}

# Función para detener el servicio si está ejecutándose
stop_service() {
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_status "Deteniendo servicio $SERVICE_NAME..."
        sudo systemctl stop $SERVICE_NAME
        print_success "Servicio detenido"
    fi
}

# Función para iniciar el servicio
start_service() {
    print_status "Iniciando servicio $SERVICE_NAME..."
    
    sudo systemctl start $SERVICE_NAME
    
    # Esperar un momento para que el servicio se inicie
    sleep 5
    
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "Servicio iniciado exitosamente"
    else
        print_error "Error al iniciar el servicio"
        print_error "Revisa los logs: sudo journalctl -u $SERVICE_NAME -f"
        exit 1
    fi
}

# Función para verificar que la aplicación está funcionando
verify_application() {
    print_status "Verificando que la aplicación está funcionando..."
    
    # Esperar un poco más para que la aplicación se inicialice completamente
    sleep 10
    
    # Verificar que el puerto está abierto
    if netstat -tlnp | grep -q ":8443 "; then
        print_success "Puerto 8443 está abierto"
    else
        print_error "Puerto 8443 no está abierto"
        print_error "Revisa los logs: sudo journalctl -u $SERVICE_NAME -f"
        exit 1
    fi
    
    # Probar conexión HTTPS (ignorar certificado autofirmado)
    if curl -k -s -o /dev/null -w "%{http_code}" https://localhost:8443/api/health | grep -q "200"; then
        print_success "Aplicación responde correctamente"
    else
        print_warning "No se pudo verificar la respuesta HTTP (puede ser normal si no hay endpoint /health)"
    fi
}

# Función para mostrar información del despliegue
show_deployment_info() {
    echo
    echo "=========================================="
    echo "🎉 DESPLIEGUE COMPLETADO"
    echo "=========================================="
    echo
    echo "📋 INFORMACIÓN DEL DESPLIEGUE:"
    echo "• Aplicación: $JAR_NAME"
    echo "• Directorio: $APP_DIR"
    echo "• Servicio: $SERVICE_NAME"
    echo "• Puerto: 8443"
    echo
    echo "🔗 ACCESO:"
    echo "• Web: https://localhost:8443"
    echo "• API: https://localhost:8443/api"
    echo
    echo "📊 COMANDOS ÚTILES:"
    echo "• Estado del servicio: sudo systemctl status $SERVICE_NAME"
    echo "• Ver logs: sudo journalctl -u $SERVICE_NAME -f"
    echo "• Reiniciar: sudo systemctl restart $SERVICE_NAME"
    echo "• Detener: sudo systemctl stop $SERVICE_NAME"
    echo
    echo "📁 ARCHIVOS IMPORTANTES:"
    echo "• JAR: $APP_DIR/target/$JAR_NAME"
    echo "• Logs: $APP_DIR/logs/lsnls.log"
    echo "• Config: $APP_DIR/src/main/resources/application.properties"
    echo
    echo "=========================================="
}

# Función para mostrar opciones de despliegue
show_deployment_options() {
    echo
    echo "🚀 OPCIONES DE DESPLIEGUE:"
    echo "1. Despliegue completo (recomendado)"
    echo "2. Solo compilar"
    echo "3. Solo reiniciar servicio"
    echo "4. Solo verificar estado"
    echo "5. Salir"
    echo
    read -p "Selecciona una opción (1-5): " choice
    
    case $choice in
        1)
            deploy_full
            ;;
        2)
            deploy_compile_only
            ;;
        3)
            deploy_restart_only
            ;;
        4)
            deploy_check_only
            ;;
        5)
            print_status "Saliendo..."
            exit 0
            ;;
        *)
            print_error "Opción inválida"
            exit 1
            ;;
    esac
}

# Función para despliegue completo
deploy_full() {
    print_status "Iniciando despliegue completo..."
    
    check_dependencies
    check_app_directory
    generate_ssl_if_needed
    check_database
    compile_application
    stop_service
    start_service
    verify_application
    show_deployment_info
}

# Función para solo compilar
deploy_compile_only() {
    print_status "Solo compilando aplicación..."
    
    check_dependencies
    check_app_directory
    generate_ssl_if_needed
    compile_application
    print_success "Compilación completada"
}

# Función para solo reiniciar servicio
deploy_restart_only() {
    print_status "Reiniciando servicio..."
    
    stop_service
    start_service
    verify_application
    print_success "Servicio reiniciado"
}

# Función para solo verificar estado
deploy_check_only() {
    print_status "Verificando estado de la aplicación..."
    
    if systemctl is-active --quiet $SERVICE_NAME; then
        print_success "Servicio $SERVICE_NAME está ejecutándose"
    else
        print_error "Servicio $SERVICE_NAME no está ejecutándose"
    fi
    
    if netstat -tlnp | grep -q ":8443 "; then
        print_success "Puerto 8443 está abierto"
    else
        print_error "Puerto 8443 no está abierto"
    fi
    
    if [[ -f "target/$JAR_NAME" ]]; then
        print_success "JAR compilado existe"
    else
        print_error "JAR compilado no existe"
    fi
}

# Función principal
main() {
    echo "🚀 Script de Despliegue LSNLS"
    echo "=============================="
    
    # Si se pasa un argumento, usar modo automático
    if [[ $# -eq 1 ]]; then
        case $1 in
            "full")
                deploy_full
                ;;
            "compile")
                deploy_compile_only
                ;;
            "restart")
                deploy_restart_only
                ;;
            "check")
                deploy_check_only
                ;;
            *)
                print_error "Argumento inválido. Uso: $0 [full|compile|restart|check]"
                exit 1
                ;;
        esac
    else
        show_deployment_options
    fi
}

# Ejecutar función principal
main "$@" 