// auth.js - Gestión de autenticación optimizada
class AuthManager {
    constructor() {
        this.token = localStorage.getItem('token');
        console.log('🔐 Constructor AuthManager - Token inicial:', this.token?.substring(0, 20) + '...');
        this.currentUser = null;
        this.isCheckingAuth = false;
        this.lastAuthCheck = 0;
        this.initializeFromStorage();
    }

    initializeFromStorage() {
        console.log('🔄 Iniciando initializeFromStorage');
        const storedUser = localStorage.getItem('usuario');
        console.log('👤 Usuario almacenado:', storedUser);
        if (storedUser && storedUser !== 'undefined') {
            try {
                this.currentUser = JSON.parse(storedUser);
                if (this.currentUser && this.currentUser.rol) {
                    this.currentUser.rol = this.normalizeRole(this.currentUser.rol);
                }
                console.log('✅ Usuario inicializado:', this.currentUser);
            } catch (error) {
                console.error('❌ Error al parsear usuario:', error);
                this.clearAuth();
            }
        } else {
            console.log('❌ No hay usuario almacenado');
            this.clearAuth();
        }
    }

    async verifyAuthentication() {
        // Si no hay token, no intentar verificar
        if (!this.token) {
            console.log('❌ No hay token almacenado');
            return false;
        }

        // Si estamos en una página pública, no verificar
        if (this.isPublicPath(window.location.pathname)) {
            return true;
        }

        try {
            console.log('🔍 Verificando autenticación...');
            console.log('Token usado:', this.token?.substring(0, 20) + '...');
            console.log('Headers:', this.getAuthHeaders());

            const response = await fetch('/api/auth/me', {
                headers: this.getAuthHeaders()
            });

            console.log('Respuesta de verificación:', {
                status: response.status,
                ok: response.ok
            });

            if (!response.ok) {
                const errorBody = await response.text();
                console.warn('❌ Error en la verificación de autenticación:', response.status);
                console.error('Respuesta de error completa:', errorBody);
                this.clearAuth();
                this.redirectToLogin();
                return false;
            }

            const user = await response.json();
            console.log('✅ Usuario verificado:', user);
            return true;
        } catch (error) {
            console.error('❌ Error de verificación:', error);
            console.error('Stack trace:', error.stack);
            this.clearAuth();
            this.redirectToLogin();
            return false;
        }
    }

    isPublicPath(path) {
        const publicPaths = [
            '/login.html',
            '/register.html',
            '/index.html',
            '/',
            '/css/',
            '/js/',
            '/images/',
            '/.well-known/',
            '/favicon.ico'
        ];
        return publicPaths.some(publicPath => path.startsWith(publicPath));
    }

    normalizeRole(role) {
        role = role.toUpperCase();
        return role.startsWith('ROLE_') ? role : 'ROLE_' + role;
    }

    clearAuth() {
        console.log('🧹 Limpiando autenticación completa...');
        localStorage.removeItem('token');
        localStorage.removeItem('usuario');
        localStorage.removeItem('rol');
        this.token = null;
        this.currentUser = null;
    }

    redirectToLogin() {
        const currentPath = window.location.pathname;
        if (!this.isPublicPath(currentPath)) {
            console.log('🔄 Redirigiendo a login...');
            window.location.href = '/login.html';
        }
    }

    isAuthenticated() {
        const hasToken = !!this.token;
        const hasUser = !!this.currentUser;
        console.log('🔐 Verificando autenticación - Token:', hasToken, 'Usuario:', hasUser);
        return hasToken && hasUser;
    }

    getToken() {
        // Siempre obtener el token más reciente de localStorage
        return localStorage.getItem('token');
    }

    getAuthHeaders() {
        console.log('📨 Generando headers de autenticación');
        console.log('Token actual:', this.token?.substring(0, 20) + '...');
        const headers = {
            'Authorization': this.token?.startsWith('Bearer ') ? this.token : `Bearer ${this.token}`,
            'Content-Type': 'application/json'
        };
        console.log('Headers generados:', headers);
        return headers;
    }

    logout() {
        this.clearAuth();
        this.redirectToLogin();
    }

    async getCurrentUser() {
        if (this.isCheckingAuth) {
            return this.currentUser;
        }

        const now = Date.now();
        if (now - this.lastAuthCheck < 1000) {
            return this.currentUser;
        }

        this.isCheckingAuth = true;
        this.lastAuthCheck = now;

        try {
            if (!this.token) {
                this.clearAuth();
                return null;
            }

            const response = await fetch('/api/auth/me', {
                headers: this.getAuthHeaders()
            });

            if (!response.ok) {
                throw new Error('Error de autenticación');
            }

            const userData = await response.json();
            this.currentUser = {
                ...userData,
                rol: this.normalizeRole(userData.rol)
            };
            
            localStorage.setItem('usuario', JSON.stringify(this.currentUser));
            return this.currentUser;
        } catch (error) {
            console.error('Error al obtener usuario:', error);
            this.clearAuth();
            return null;
        } finally {
            this.isCheckingAuth = false;
        }
    }

    async checkPermissions() {
        const user = await this.getCurrentUser();
        if (!user) return null;

        const userRole = user.rol.replace('ROLE_', '').toLowerCase();
        return {
            canRead: true,
            canCreate: ['admin', 'guion', 'verificacion', 'direccion'].includes(userRole),
            canEdit: ['admin', 'guion', 'verificacion', 'direccion'].includes(userRole),
            canDelete: ['admin', 'direccion'].includes(userRole),
            canValidate: ['admin', 'direccion'].includes(userRole),
            canVerify: ['admin', 'verificacion', 'direccion'].includes(userRole),
            rol: user.rol
        };
    }

    hasRole(role) {
        if (!this.currentUser || !this.currentUser.rol) {
            return false;
        }
        return this.currentUser.rol === role;
    }
}

// Instancia global
const authManager = new AuthManager();

// Funciones de autenticación
async function iniciarSesion(event) {
    event.preventDefault();
    console.log('Iniciando proceso de login...');
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    if (!username || !password) {
        console.warn('Campos incompletos');
        mostrarError('Usuario y contraseña son requeridos');
        return;
    }

    try {
        console.log('Enviando petición de login con usuario:', username);
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ nombre: username, password })
        });

        console.log('Respuesta recibida:', response.status);
        if (!response.ok) {
            const errorData = await response.json();
            console.error('Error de login:', errorData);
            throw new Error(errorData.mensaje || errorData.error || 'Error en las credenciales');
        }

        const data = await response.json();
        console.log('Login exitoso, datos recibidos:', {
            id: data.id,
            nombre: data.nombre,
            rol: data.rol,
            tipo: data.tipo,
            tokenLength: data.token?.length
        });
        
        if (!data.token) {
            throw new Error('Token no recibido en la respuesta');
        }

        // Guardar token y datos de usuario
        const token = data.token;
        console.log('Token a guardar:', token.substring(0, 20) + '...');
        localStorage.setItem('token', token);
        
        const usuario = {
            id: data.id,
            nombre: data.nombre,
            rol: authManager.normalizeRole(data.rol)
        };
        console.log('Datos de usuario a guardar:', usuario);
        localStorage.setItem('usuario', JSON.stringify(usuario));
        
        // Actualizar el estado del AuthManager
        authManager.token = token;
        authManager.currentUser = usuario;
        
        console.log('Datos guardados correctamente');
        console.log('Token en AuthManager:', authManager.token?.substring(0, 20) + '...');
        console.log('Usuario en AuthManager:', authManager.currentUser);
        mostrarExito('Login exitoso');
        
        // Verificar que los datos se guardaron
        const storedToken = localStorage.getItem('token');
        const storedUser = localStorage.getItem('usuario');
        
        console.log('Verificando datos almacenados:');
        console.log('- Token almacenado:', storedToken?.substring(0, 20) + '...');
        console.log('- Usuario almacenado:', JSON.parse(storedUser));
        
        if (!storedToken || !storedUser) {
            throw new Error('Error al guardar los datos de sesión');
        }

        // Hacer una petición de prueba antes de redirigir
        console.log('Realizando petición de prueba a /api/auth/me');
        console.log('Headers de la petición:', authManager.getAuthHeaders());
        
        const testResponse = await fetch('/api/auth/me', {
            headers: authManager.getAuthHeaders()
        });

        console.log('Respuesta de verificación:', {
            status: testResponse.status,
            ok: testResponse.ok
        });

        if (!testResponse.ok) {
            const errorBody = await testResponse.text();
            console.error('Respuesta de error completa:', errorBody);
            throw new Error('Error al verificar la autenticación');
        }

        const verifiedUser = await testResponse.json();
        console.log('Usuario verificado:', verifiedUser);

        // Si todo está bien, redirigir
        console.log('Redirigiendo a preguntas.html...');
        window.location.href = '/preguntas.html';
    } catch (error) {
        console.error('Error completo:', error);
        console.error('Stack trace:', error.stack);
        mostrarError('Error al iniciar sesión: ' + error.message);
        // Limpiar datos en caso de error
        authManager.clearAuth();
    }
}

function cerrarSesion() {
    // Guardar configuración de columnas antes de limpiar
    const configColumnas = localStorage.getItem('configuracionColumnasConcursantes');
    
    // Limpiar solo los datos de autenticación
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    localStorage.removeItem('rol');
    
    // Restaurar configuración de columnas si existía
    if (configColumnas) {
        localStorage.setItem('configuracionColumnasConcursantes', configColumnas);
    }
    
    window.location.href = 'login.html';
}

// Funciones de utilidad
function mostrarError(mensaje) {
    if (typeof Utils !== 'undefined' && Utils.mostrarToastError) {
        Utils.mostrarToastError(mensaje);
        return;
    }
    Toastify({
        text: mensaje,
        duration: 3000,
        close: true,
        gravity: 'top',
        position: 'right',
        style: { background: 'linear-gradient(to right, #ff0000, #cc0000)' }
    }).showToast();
}

function mostrarExito(mensaje) {
    if (typeof Utils !== 'undefined' && Utils.mostrarToastExito) {
        Utils.mostrarToastExito(mensaje);
        return;
    }
    Toastify({
        text: mensaje,
        duration: 3000,
        close: true,
        gravity: 'top',
        position: 'right',
        style: { background: 'linear-gradient(to right, #00b09b, #96c93d)' }
    }).showToast();
}

// Inicialización al cargar la página
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Inicializando autenticación...');
    
    // Conectar el formulario de login si existe
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', iniciarSesion);
    }
    
    // Si estamos en una página protegida, verificar autenticación
    if (!authManager.isPublicPath(window.location.pathname)) {
        const isAuthenticated = await authManager.verifyAuthentication();
        if (!isAuthenticated) {
            authManager.redirectToLogin();
        }
    }
}); 