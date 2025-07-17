// administracion.js

// Verificar que solo admin puede acceder
document.addEventListener('DOMContentLoaded', async function() {
    const usuarioActual = JSON.parse(localStorage.getItem('usuario'));
    if (!usuarioActual || usuarioActual.rol !== 'ROLE_ADMIN') {
        window.location.href = 'login.html';
        return;
    }
    await cargarUsuarios();
    
    // Agregar evento al formulario de crear usuario
    document.getElementById('formCrearUsuario').addEventListener('submit', async function(e) {
        e.preventDefault();
        await procesarCreacionUsuario();
    });
});

async function cargarUsuarios() {
    try {
        const usuarios = await apiManager.getUsuarios();
        renderTablaUsuarios(usuarios);
    } catch (e) {
        alert('Error cargando usuarios: ' + e.message);
    }
}

function renderTablaUsuarios(usuarios) {
    const tbody = document.getElementById('tabla-usuarios');
    tbody.innerHTML = '';
    usuarios.forEach(usuario => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${usuario.id}</td>
            <td><input type="text" value="${usuario.nombre}" data-id="${usuario.id}" class="input-nombre form-control form-control-sm" onchange="guardarUsuario(${usuario.id})"></td>
            <td>
                <select class="form-select form-select-sm input-rol" data-id="${usuario.id}" onchange="guardarUsuario(${usuario.id})">
                    <option value="ROLE_ADMIN" ${usuario.rol === 'ROLE_ADMIN' ? 'selected' : ''}>Admin</option>
                    <option value="ROLE_CONSULTA" ${usuario.rol === 'ROLE_CONSULTA' ? 'selected' : ''}>Consulta</option>
                    <option value="ROLE_GUION" ${usuario.rol === 'ROLE_GUION' ? 'selected' : ''}>Guion</option>
                    <option value="ROLE_VERIFICACION" ${usuario.rol === 'ROLE_VERIFICACION' ? 'selected' : ''}>Verificación</option>
                    <option value="ROLE_DIRECCION" ${usuario.rol === 'ROLE_DIRECCION' ? 'selected' : ''}>Dirección</option>
                </select>
            </td>
            <td>
                <button class="btn btn-sm btn-warning" onclick="resetearPassword(${usuario.id})">🔑</button>
                <button class="btn btn-sm btn-danger" onclick="eliminarUsuario(${usuario.id})">🗑️</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function guardarUsuario(id) {
    const nombre = document.querySelector(`.input-nombre[data-id='${id}']`).value;
    const rol = document.querySelector(`.input-rol[data-id='${id}']`).value;
    const usuario = { nombre, rol };
    
    try {
        await apiManager.updateUsuario(id, usuario);
        alert('Usuario actualizado');
        await cargarUsuarios();
    } catch (e) {
        alert('Error actualizando usuario: ' + e.message);
    }
}

async function eliminarUsuario(id) {
    if (!confirm('¿Seguro que quieres eliminar este usuario?')) return;
    try {
        await apiManager.deleteUsuario(id);
        alert('Usuario eliminado');
        await cargarUsuarios();
    } catch (e) {
        alert('Error eliminando usuario: ' + e.message);
    }
}

async function resetearPassword(id) {
    if (!confirm('¿Resetear la contraseña a 123456?')) return;
    try {
        await apiManager.resetPasswordUsuario(id);
        alert('Contraseña reseteada a 123456');
    } catch (e) {
        alert('Error reseteando contraseña: ' + e.message);
    }
}

function crearUsuario() {
    console.log('crearUsuario() llamada'); // Debug
    
    // Verificar que el modal existe
    const modalElement = document.getElementById('modal-usuario');
    if (!modalElement) {
        console.error('Modal no encontrado');
        alert('Error: Modal no encontrado');
        return;
    }
    
    // Limpiar formulario 
    document.getElementById('formCrearUsuario').reset();
    // Restaurar valor de contraseña después del reset
    document.getElementById('password-usuario').value = '123456';
    
    // Mostrar modal
    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

async function procesarCreacionUsuario() {
    const formData = new FormData(document.getElementById('formCrearUsuario'));
    const usuario = {
        nombre: formData.get('nombre'),
        rol: formData.get('rol')
        // NO enviamos password - se asigna automáticamente en el backend
    };
    
    try {
        await apiManager.createUsuario(usuario);
        alert(`Usuario "${usuario.nombre}" creado exitosamente.\n\nCredenciales de acceso:\n• Usuario: ${usuario.nombre}\n• Contraseña: 123456\n\nEl usuario podrá cambiar su contraseña después del primer login.`);
        
        // Cerrar modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modal-usuario'));
        modal.hide();
        
        // Recargar usuarios
        await cargarUsuarios();
    } catch (e) {
        alert('Error creando usuario: ' + e.message);
    }
}

window.cambiarPassword = function() {
    document.getElementById('form-cambiar-password').reset();
    const modal = new bootstrap.Modal(document.getElementById('modal-cambiar-password'));
    modal.show();
}; 