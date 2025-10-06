// Función para ir a una página específica
async function irAPagina(pagina) {
    console.log('🔄 [PAGINACIÓN] Navegando a página:', pagina);
    
    if (cargando) {
        console.log('❌ [PAGINACIÓN] Ya está cargando, abortando...');
        return;
    }
    
    if (pagina < 0 || pagina >= totalPaginas || pagina === paginaActual) {
        console.log('❌ [PAGINACIÓN] Página inválida o es la página actual:', pagina);
        return;
    }
    
    // Guardar la posición actual del scroll
    const scrollPosition = window.scrollY;
    
    // Actualizar la página actual y cargar los datos
    paginaActual = pagina;
    await cargarConcursantes(true);
    
    // Restaurar la posición del scroll al inicio de la tabla
    setTimeout(() => {
        const tabla = document.getElementById('tabla-concursantes-principal');
        if (tabla) {
            tabla.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }, 100);
}

// Reemplazar la función actualizarPaginacion
function actualizarPaginacion() {
    console.log('🔄 [PAGINACION] Actualizando paginación con botones de páginas...');
    console.log('🔄 [PAGINACION] Estado - concursantes.length:', concursantes.length, 'totalConcursantes:', totalConcursantes, 'paginaActual:', paginaActual, 'totalPaginas:', totalPaginas);
    
    // Usar el contenedor de paginación del HTML
    const paginacionContainer = document.getElementById('paginacion-concursantes');
    const infoPaginacion = document.getElementById('info-paginacion-concursantes');
    
    if (!paginacionContainer) {
        console.error('❌ [PAGINACION] Contenedor de paginación no encontrado');
        return;
    }

    // Actualizar información de paginación
    if (infoPaginacion) {
        const inicio = (paginaActual * tamanioPagina) + 1;
        const fin = Math.min((paginaActual + 1) * tamanioPagina, totalConcursantes);
        infoPaginacion.textContent = `Mostrando ${inicio}-${fin} de ${totalConcursantes} concursantes`;
    }

    // Limpiar botones existentes
    paginacionContainer.innerHTML = '';

    if (totalPaginas <= 1) {
        console.log('✅ [PAGINACION] Solo hay una página, no se muestran botones');
        return;
    }

    // Crear botón "Primera"
    const primeraPagina = document.createElement('li');
    primeraPagina.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    primeraPagina.innerHTML = `<a class="page-link" href="#" onclick="irAPagina(0)">Primera</a>`;
    paginacionContainer.appendChild(primeraPagina);

    // Crear botón "Anterior"
    const paginaAnterior = document.createElement('li');
    paginaAnterior.className = `page-item ${paginaActual === 0 ? 'disabled' : ''}`;
    paginaAnterior.innerHTML = `<a class="page-link" href="#" onclick="irAPagina(${paginaActual - 1})">Anterior</a>`;
    paginacionContainer.appendChild(paginaAnterior);

    // Calcular rango de páginas a mostrar
    const inicio = Math.max(0, paginaActual - 2);
    const fin = Math.min(totalPaginas - 1, paginaActual + 2);

    // Mostrar páginas en el rango
    for (let i = inicio; i <= fin; i++) {
        const pagina = document.createElement('li');
        pagina.className = `page-item ${i === paginaActual ? 'active' : ''}`;
        pagina.innerHTML = `<a class="page-link" href="#" onclick="irAPagina(${i})">${i + 1}</a>`;
        paginacionContainer.appendChild(pagina);
    }

    // Crear botón "Siguiente"
    const paginaSiguiente = document.createElement('li');
    paginaSiguiente.className = `page-item ${paginaActual >= totalPaginas - 1 ? 'disabled' : ''}`;
    paginaSiguiente.innerHTML = `<a class="page-link" href="#" onclick="irAPagina(${paginaActual + 1})">Siguiente</a>`;
    paginacionContainer.appendChild(paginaSiguiente);

    // Crear botón "Última"
    const ultimaPagina = document.createElement('li');
    ultimaPagina.className = `page-item ${paginaActual >= totalPaginas - 1 ? 'disabled' : ''}`;
    ultimaPagina.innerHTML = `<a class="page-link" href="#" onclick="irAPagina(${totalPaginas - 1})">Última</a>`;
    paginacionContainer.appendChild(ultimaPagina);

    console.log('✅ [PAGINACION] Botones de paginación creados correctamente');
}