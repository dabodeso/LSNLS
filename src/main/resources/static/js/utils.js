// utils.js - Utilidades optimizadas
class Utils {
    // Mostrar alertas optimizado
    static showAlert(message, type = 'info', duration = 4000) {
        if (type === 'success') {
            Utils.mostrarToastExito(message, duration);
            return;
        }
        if (type === 'danger' || type === 'error') {
            Utils.mostrarToastError(message, duration);
            return;
        }
        Utils._showBootstrapAlert(message, type, duration);
    }

    static _showBootstrapAlert(message, type = 'info', duration = 4000) {
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
            let d;
            
            // Si la fecha viene en formato ISO (YYYY-MM-DD)
            if (typeof fechaString === 'string' && fechaString.includes('-')) {
                const partes = fechaString.split('-');
                if (partes.length === 3) {
                    const año = parseInt(partes[0]);
                    const mes = parseInt(partes[1]) - 1; // Los meses en JavaScript van de 0-11
                    const dia = parseInt(partes[2]);
                    d = new Date(año, mes, dia);
                } else {
                    d = new Date(fechaString);
                }
            } else {
                d = new Date(fechaString);
            }
            
            // Verificar si la fecha es válida
            if (isNaN(d.getTime())) {
                console.warn('⚠️ Fecha inválida:', fechaString);
                return 'Fecha inválida';
            }
            
            const dia = String(d.getDate()).padStart(2, '0');
            const mes = String(d.getMonth() + 1).padStart(2, '0');
            const anio = d.getFullYear();
            
            // Solo mostrar hora si no es 00:00
            let hora = d.getHours();
            let min = d.getMinutes();
            let horaStr = '';
            if (!isNaN(hora) && !isNaN(min) && (hora !== 0 || min !== 0)) {
                horaStr = ' ' + String(hora).padStart(2, '0') + ':' + String(min).padStart(2, '0');
            }
            
            return `${dia}/${mes}/${anio}${horaStr}`;
        } catch (error) {
            console.warn('⚠️ Error al formatear fecha:', fechaString, error);
            return 'Fecha inválida';
        }
    }

    // Truncar texto optimizado
    static truncateText(text, maxLength) {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    // Formatear niveles para UI sin el "_" interno de la BBDD
    static formatearNivel(nivel) {
        if (nivel == null) return '';
        const valor = String(nivel);
        if (valor === '_0') return '0';
        return valor.replace(/^_+/, '');
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
                'grabado': 'bg-info',
                'asignado_jornada': 'bg-warning',
                'asignado_concursantes': 'bg-dark'
            },
            combo: {
                'borrador': 'bg-secondary',
                'creado': 'bg-primary',
                'adjudicado': 'bg-success',
                'grabado': 'bg-info',
                'asignado_jornada': 'bg-warning',
                'asignado_concursantes': 'bg-dark'
            }
        };
        
        return classes[tipo]?.[estado] || 'bg-secondary';
    }

    // Convertir estados de cuestionarios a texto legible en español
    static formatearEstadoCuestionario(estado) {
        const estadosEspanol = {
            'borrador': 'Borrador',
            'creado': 'Creado',
            'adjudicado': 'Adjudicado', 
            'grabado': 'Grabado',
            'asignado_jornada': 'Asignado a Jornada',
            'asignado_concursantes': 'Asignado a Concursantes'
        };
        
        return estadosEspanol[estado] || estado.charAt(0).toUpperCase() + estado.slice(1);
    }

    // Convertir estados de combos a texto legible en español
    static formatearEstadoCombo(estado) {
        const estadosEspanol = {
            'borrador': 'Borrador',
            'creado': 'Creado',
            'adjudicado': 'Adjudicado', 
            'grabado': 'Grabado',
            'asignado_jornada': 'Asignado a Jornada',
            'asignado_concursantes': 'Asignado a Concursantes'
        };
        
        return estadosEspanol[estado] || estado.charAt(0).toUpperCase() + estado.slice(1);
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

    /** Abre una ruta relativa en una pestaña nueva sin perder la vista actual. */
    static abrirEnNuevaPestana(ruta) {
        if (!ruta) return;
        window.open(ruta, '_blank', 'noopener,noreferrer');
    }

    static estiloToastExito() {
        return { background: 'linear-gradient(to right, #00b09b, #96c93d)' };
    }

    static estiloToastError() {
        return { background: 'linear-gradient(to right, #ff0000, #cc0000)' };
    }

    static estiloToastAviso() {
        return { background: 'linear-gradient(to right, #ffc107, #ff9800)' };
    }

    static mostrarToastExito(mensaje, duration = 3000) {
        if (typeof Toastify === 'function') {
            Toastify({
                text: mensaje,
                duration,
                close: true,
                gravity: 'top',
                position: 'right',
                style: Utils.estiloToastExito()
            }).showToast();
            return;
        }
        Utils._showBootstrapAlert(mensaje, 'success', duration);
    }

    static mostrarToastError(mensaje, duration = 4000) {
        if (typeof Toastify === 'function') {
            Toastify({
                text: mensaje,
                duration,
                close: true,
                gravity: 'top',
                position: 'right',
                style: Utils.estiloToastError()
            }).showToast();
            return;
        }
        Utils._showBootstrapAlert(mensaje, 'danger', duration);
    }

    /** Detecta textos genéricos del servidor (p. ej. Spring: "Forbidden"). */
    static esMensajeHttpGenerico(texto) {
        if (texto == null || typeof texto !== 'string') return true;
        const t = texto.trim().toLowerCase();
        if (!t) return true;
        const genericos = new Set([
            'forbidden',
            'access denied',
            'access_denied',
            'unauthorized',
            'bad request',
            'not found',
            'method not allowed',
            'internal server error',
            'conflict',
            'payload too large',
            'unsupported media type',
            'too many requests',
            'service unavailable',
            'gateway timeout',
            'error',
            'unknown error',
            'no tienes permisos para realizar esta acción.',
            'no tienes permisos para realizar esta accion.'
        ]);
        if (genericos.has(t)) return true;
        if (/^(error|http error)\s*\d{0,3}$/i.test(t)) return true;
        return false;
    }

    /** Extrae un mensaje útil del cuerpo de error (texto plano o JSON de Spring). */
    static extraerDetalleErrorCuerpo(text) {
        if (!text || !String(text).trim()) return '';
        const trimmed = String(text).trim();
        try {
            const json = JSON.parse(trimmed);
            const candidatos = [json.message, json.mensaje, json.detail, json.error, json.title];
            for (const candidato of candidatos) {
                if (typeof candidato === 'string' && candidato.trim() && !Utils.esMensajeHttpGenerico(candidato)) {
                    return candidato.trim();
                }
            }
        } catch {
            if (!Utils.esMensajeHttpGenerico(trimmed)) {
                return trimmed;
            }
        }
        return '';
    }

    /** Extrae mensaje legible de una Response HTTP fallida (texto plano o JSON). */
    static async mensajeDesdeResponse(response, accion = 'realizar esta acción') {
        let detalle = '';
        try {
            const text = await response.text();
            detalle = Utils.extraerDetalleErrorCuerpo(text);
        } catch {
            // ignorar errores de lectura del body
        }
        return Utils.mensajeErrorHttp(response.status, detalle, accion);
    }

    /** Mensajes HTTP estándar; prioriza el detalle del backend si es claro. */
    static mensajeErrorHttp(status, detalle, accion = 'realizar esta acción') {
        const texto = Utils.extraerDetalleErrorCuerpo(detalle) || (typeof detalle === 'string' ? detalle.trim() : '');
        if (texto && !Utils.esMensajeHttpGenerico(texto)) {
            return texto;
        }
        switch (status) {
            case 401:
                return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
            case 403:
                return `No tienes permisos para ${accion}.`;
            case 404:
                return 'El recurso solicitado no existe.';
            case 409:
                return 'Conflicto: la operación no se puede completar en el estado actual.';
            case 413:
                return 'El archivo es demasiado grande. El tamaño máximo permitido es 10MB.';
            case 422:
                return 'Los datos enviados no son válidos.';
            case 500:
                return 'Error interno del servidor. Inténtalo de nuevo más tarde.';
            default:
                return `Error al ${accion}.`;
        }
    }

    /** Normaliza errores de fetch directo o de apiManager a mensaje para el usuario. */
    static mensajeErrorApi(error, accion = 'realizar esta acción') {
        if (!error) {
            return `Error al ${accion}.`;
        }
        const msg = (error.message || String(error)).trim();
        if (!msg) {
            return `Error al ${accion}.`;
        }
        if (msg.includes('UNAUTHORIZED')) {
            return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
        }
        const match = msg.match(/^(\d{3}):\s*(.*)$/s);
        if (match) {
            const status = parseInt(match[1], 10);
            const detalle = match[2].trim();
            return Utils.mensajeErrorHttp(status, detalle, accion);
        }
        const detalle = Utils.extraerDetalleErrorCuerpo(msg);
        if (detalle && !Utils.esMensajeHttpGenerico(detalle)) {
            return detalle;
        }
        if (!Utils.esMensajeHttpGenerico(msg)) {
            return msg;
        }
        return `Error al ${accion}.`;
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
    const usuario = JSON.parse(localStorage.getItem('usuario') || '{}');
    const navAdmin = document.getElementById('nav-admin');
    if (usuario && usuario.rol === 'ROLE_ADMIN' && navAdmin) {
        navAdmin.style.display = '';
    } else if (navAdmin) {
        navAdmin.style.display = 'none';
    }
    // Mostrar nombre de usuario
    const usuarioActual = document.getElementById('usuario-actual');
    if (usuarioActual && usuario && usuario.nombre) {
        usuarioActual.textContent = usuario.nombre;
    }
}

// Funciones para mostrar mensajes de error y éxito
function mostrarError(mensaje) {
    Utils.mostrarToastError(mensaje);
}

function mostrarExito(mensaje) {
    Utils.mostrarToastExito(mensaje);
}

console.log('🛠️ Utils cargado y optimizado');

/**
 * Recarga la vista de la página actual manteniendo filtros y paginación activos.
 */
window.refrescarPaginaActual = async function refrescarPaginaActual() {
    const path = (window.location.pathname || '').toLowerCase();
    try {
        if (path.includes('pregunta')) {
            if (window.PreguntasManager?.recargarConFiltros) {
                await PreguntasManager.recargarConFiltros();
            }
        } else if (path.includes('cuestionario')) {
            if (window.CuestionariosManager?.recargarConFiltros) {
                await CuestionariosManager.recargarConFiltros();
            }
        } else if (path.includes('combo')) {
            if (window.CombosManager?.recargarConFiltros) {
                await CombosManager.recargarConFiltros();
            }
        } else if (path.includes('concursante')) {
            if (typeof cargarConcursantes === 'function') {
                await cargarConcursantes(false);
            }
        } else if (path.includes('programa')) {
            if (typeof refrescarVistaProgramas === 'function') {
                await refrescarVistaProgramas();
            } else if (typeof recargarProgramas === 'function') {
                await recargarProgramas();
            }
        } else if (path.includes('jornada')) {
            if (window.JornadasManager?.recargarConFiltros) {
                await JornadasManager.recargarConFiltros();
            }
        } else if (path.includes('administracion')) {
            if (typeof cargarUsuarios === 'function') {
                await cargarUsuarios();
            }
        }
    } catch (e) {
        console.error('[refrescarPaginaActual] Error:', e);
    }
};