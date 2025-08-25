// auth-guard.js - Protección de rutas mejorada
(function () {
    const token = localStorage.getItem('token');
    const usuario = localStorage.getItem('usuario');



    // Si no hay token o usuario, redirigir al login
    if (!token || !usuario) {
        window.location.href = '/login.html';
        return;
    }

    // Función para verificar si el token está expirado
    function isTokenExpired(token) {
        try {
            // Verificar que el token tenga el formato correcto (3 partes separadas por puntos)
            if (!token || typeof token !== 'string' || token.split('.').length !== 3) {
                return true;
            }
            
            const payload = JSON.parse(atob(token.split('.')[1]));
            const now = Date.now() / 1000;
            
            // Verificar que el payload tenga la propiedad exp
            if (!payload || !payload.exp) {
                return true;
            }
            
            const isExpired = payload.exp < now;
            return isExpired;
        } catch (error) {
            return true;
        }
    }

    // Verificar si el token está expirado
    if (isTokenExpired(token)) {
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
    console.log('🔍 [AUTH-GUARD] Token válido, continuando...');
})(); 