// auth-guard.js - Protección de rutas mejorada
(function() {
    const token = localStorage.getItem('token');
    const usuario = localStorage.getItem('usuario');
    
    // Si no hay token o usuario, redirigir al login
    if (!token || !usuario) {
        console.log('🚪 No hay token o usuario, redirigiendo al login...');
        window.location.href = '/login.html';
        return;
    }
    
    // Función para verificar si el token está expirado
    function isTokenExpired(token) {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const now = Date.now() / 1000;
            return payload.exp < now;
        } catch (error) {
            console.log('🔍 Error al decodificar token, considerando expirado:', error);
            return true;
        }
    }
    
    // Verificar si el token está expirado
    if (isTokenExpired(token)) {
        console.log('⏰ Token expirado, limpiando sesión y redirigiendo al login...');
        localStorage.removeItem('token');
        localStorage.removeItem('usuario');
        localStorage.removeItem('rol');
        window.location.href = '/login.html';
        return;
    }
    
    // Configurar interceptor global para errores 401
    const originalFetch = window.fetch;
    window.fetch = async (...args) => {
        try {
            const response = await originalFetch(...args);
            
            // Si recibimos 401, es probable que el token haya expirado
            if (response.status === 401) {
                console.log('🚫 Error 401 detectado, limpiando sesión y redirigiendo al login...');
                localStorage.removeItem('token');
                localStorage.removeItem('usuario');
                localStorage.removeItem('rol');
                window.location.href = '/login.html';
                return response;
            }
            
            return response;
        } catch (error) {
            throw error;
        }
    };
    
    console.log('🛡️ Auth guard activado - Token válido para usuario:', usuario);
})(); 