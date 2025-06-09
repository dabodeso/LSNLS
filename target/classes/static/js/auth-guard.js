// auth-guard.js
(function() {
    const token = localStorage.getItem('token');
    const usuario = localStorage.getItem('usuario');
    if (!token || !usuario) {
        window.location.href = '/login.html';
    }
})(); 