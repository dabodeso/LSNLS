// utils.js - Utilidades optimizadas
class Utils {
    // Mostrar alertas optimizado
    static showAlert(message, type = 'info', duration = 4000) {
        console.log(`🔔 Alerta ${type.toUpperCase()}: ${message}`);
        
        // Crear el contenedor de alertas si no existe
        let alertContainer = document.getElementById('alertContainer');
        if (!alertContainer) {
            alertContainer = document.createElement('div');
            alertContainer.id = 'alertContainer';
            alertContainer.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            `;
            document.body.appendChild(alertContainer);
        }

        // Crear la alerta
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        // Agregar al contenedor
        alertContainer.appendChild(alertDiv);

        // Auto-remover después del tiempo especificado
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.remove();
            }
        }, duration);
    }

    // Formatear fecha optimizado
    static formatearFecha(fechaString) {
        if (!fechaString) return 'N/A';
        try {
            const d = new Date(fechaString);
            const dia = String(d.getDate()).padStart(2, '0');
            const mes = String(d.getMonth() + 1).padStart(2, '0');
            const anio = d.getFullYear();
            let hora = d.getHours();
            let min = d.getMinutes();
            let horaStr = '';
            if (!isNaN(hora) && !isNaN(min) && (hora !== 0 || min !== 0)) {
                horaStr = ' ' + String(hora).padStart(2, '0') + ':' + String(min).padStart(2, '0');
            }
            return `${dia}/${mes}/${anio}${horaStr}`;
        } catch (error) {
            console.warn('⚠️ Fecha inválida:', fechaString);
            return 'Fecha inválida';
        }
    }

    // Truncar texto optimizado
    static truncateText(text, maxLength) {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    // Obtener clase CSS para badges de estado optimizado
    static getEstadoBadgeClass(estado, tipo = 'pregunta') {
        const classes = {
            pregunta: {
                'borrador': 'bg-secondary',
                'para_verificar': 'bg-primary',
                'verificada': 'bg-info',
                'aprobada': 'bg-success',
                'rechazada': 'bg-danger',
                'revisar': 'bg-warning',
                'corregir': 'bg-warning'
            },
            cuestionario: {
                'borrador': 'bg-secondary',
                'creado': 'bg-primary',
                'adjudicado': 'bg-success',
                'grabado': 'bg-info'
            }
        };
        
        return classes[tipo]?.[estado] || 'bg-secondary';
    }

    // Debounce para optimizar búsquedas
    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Verificar permisos y mostrar mensaje si no tiene acceso
    static async checkPermissionAndAlert(permission, action) {
        const permissions = await authManager.checkPermissions();
        if (!permissions || !permissions[permission]) {
            const roleRequired = Utils.getRoleRequiredForPermission(permission);
            Utils.showAlert(`No tienes permisos para ${action}. Se requiere rol: ${roleRequired}`, 'warning');
            return false;
        }
        return true;
    }

    // Obtener rol requerido para un permiso
    static getRoleRequiredForPermission(permission) {
        const roleMap = {
            canDelete: 'DIRECCION',
            canValidate: 'DIRECCION',
            canVerify: 'VERIFICACION o DIRECCION',
            canCreate: 'GUION, VERIFICACION o DIRECCION',
            canEdit: 'GUION, VERIFICACION o DIRECCION'
        };
        return roleMap[permission] || 'DESCONOCIDO';
    }

    // Confirmar acción destructiva
    static confirmDestructiveAction(message, title = 'Confirmar acción') {
        return confirm(`${title}\n\n${message}\n\nEsta acción no se puede deshacer.`);
    }

    // Sanitizar HTML
    static sanitizeHTML(str) {
        const temp = document.createElement('div');
        temp.textContent = str;
        return temp.innerHTML;
    }
}

// Funciones globales para compatibilidad
function showAlert(message, type, duration) {
    Utils.showAlert(message, type, duration);
}

function formatearFecha(fechaString) {
    return Utils.formatearFecha(fechaString);
}

function truncateText(text, maxLength) {
    return Utils.truncateText(text, maxLength);
}

function mostrarBotonAdminNavbar() {
    const usuario = JSON.parse(localStorage.getItem('usuario'));
    const navAdmin = document.getElementById('nav-admin');
    if (usuario && usuario.rol === 'ROLE_ADMIN' && navAdmin) {
        navAdmin.style.display = '';
    } else if (navAdmin) {
        navAdmin.style.display = 'none';
    }
}

// Funciones para mostrar mensajes de error y éxito
function mostrarError(mensaje) {
    Utils.showAlert(mensaje, 'danger', 5000);
}

function mostrarExito(mensaje) {
    Utils.showAlert(mensaje, 'success', 3000);
}

console.log('🛠️ Utils cargado y optimizado'); 