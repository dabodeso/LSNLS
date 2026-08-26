(function () {
    const root = document.getElementById('lsnls-navbar-root');
    if (!root) {
        return;
    }
    const page = (window.location.pathname.split('/').pop() || '').toLowerCase() || 'preguntas.html';
    const activo = function (href) {
        return page === href ? ' active' : '';
    };
    root.innerHTML =
        '<nav class="navbar navbar-expand-lg navbar-dark bg-dark">' +
        '  <div class="container-fluid">' +
        '    <a class="navbar-brand" href="preguntas.html">LSNLS</a>' +
        '    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">' +
        '      <span class="navbar-toggler-icon"></span>' +
        '    </button>' +
        '    <div class="collapse navbar-collapse" id="navbarNav">' +
        '      <ul class="navbar-nav">' +
        '        <li class="nav-item"><a class="nav-link' + activo('preguntas.html') + '" href="preguntas.html">Preguntas</a></li>' +
        '        <li class="nav-item"><a class="nav-link' + activo('cuestionarios.html') + '" href="cuestionarios.html">Cuestionarios</a></li>' +
        '        <li class="nav-item"><a class="nav-link' + activo('combos.html') + '" href="combos.html">Combos</a></li>' +
        '        <li class="nav-item"><a class="nav-link' + activo('jornadas.html') + '" href="jornadas.html">Jornadas</a></li>' +
        '        <li class="nav-item"><a class="nav-link' + activo('concursantes.html') + '" href="concursantes.html">Concursantes</a></li>' +
        '        <li class="nav-item"><a class="nav-link' + activo('programas.html') + '" href="programas.html">Programas</a></li>' +
        '        <li class="nav-item" id="nav-admin" style="display:none"><a class="nav-link' + activo('administracion.html') + '" href="administracion.html">Administración</a></li>' +
        '      </ul>' +
        '      <ul class="navbar-nav ms-auto">' +
        '        <li class="nav-item dropdown">' +
        '          <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" id="usuario-menu" role="button" data-bs-toggle="dropdown" aria-expanded="false">' +
        '            <span id="usuario-actual"></span>' +
        '            <span style="font-size: 1.5em; margin-left: 8px;"><i class="fas fa-bars"></i></span>' +
        '          </a>' +
        '          <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="usuario-menu">' +
        '            <li><a class="dropdown-item" href="#" onclick="cerrarSesion()">Cerrar sesión</a></li>' +
        '            <li><a class="dropdown-item" href="#" onclick="cambiarPassword()">Cambiar contraseña</a></li>' +
        '          </ul>' +
        '        </li>' +
        '      </ul>' +
        '    </div>' +
        '  </div>' +
        '</nav>';
    if (page === 'administracion.html') {
        const navAdmin = document.getElementById('nav-admin');
        if (navAdmin) {
            navAdmin.style.display = '';
        }
    }
})();
