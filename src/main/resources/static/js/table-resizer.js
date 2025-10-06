/**
 * Sistema de redimensionamiento de columnas para tablas
 * Permite redimensionar columnas arrastrando los bordes de los encabezados
 */

class TableResizer {
    constructor(tableId, options = {}) {
        this.table = document.getElementById(tableId);
        this.options = {
            minWidth: 50,
            maxWidth: null, // Sin límite máximo
            resizeHandleWidth: 4,
            ...options
        };
        
        if (!this.table) {
            console.error(`Tabla con ID '${tableId}' no encontrada`);
            return;
        }
        
        this.init();
    }
    
    init() {
        this.createResizeHandles();
        this.bindEvents();
        this.setDefaultColumnWidths();
    }
    
    /**
     * Establece anchos por defecto para columnas específicas
     */
    setDefaultColumnWidths() {
        const thead = this.table.querySelector('thead tr');
        if (!thead) return;
        
        const columns = thead.querySelectorAll('th');
        columns.forEach((th, index) => {
            const columnName = th.textContent.trim().toLowerCase();
            let defaultWidth = 120; // Ancho por defecto
            
            // Calcular ancho mínimo basado en el contenido del encabezado
            const headerText = th.textContent.trim();
            const textWidth = headerText.length * 10; // 10px por carácter para mejor legibilidad
            const baseWidth = Math.max(textWidth, 100); // Mínimo 100px para mejor legibilidad
            
            // Ajustar anchos según el tipo de columna, pero siempre respetando el ancho del texto
            if (columnName.includes('id') || columnName.includes('nº') || columnName.includes('#')) {
                defaultWidth = Math.max(baseWidth, 100);
            } else if (columnName.includes('edad') || columnName.includes('duración') || columnName.includes('duracion')) {
                defaultWidth = Math.max(baseWidth, 120);
            } else if (columnName.includes('combo') || columnName.includes('x') || columnName.includes('factor')) {
                defaultWidth = Math.max(baseWidth, 100);
            } else if (columnName.includes('valoración') || columnName.includes('valoracion')) {
                defaultWidth = Math.max(baseWidth, 160);
            } else if (columnName.includes('pregunta')) {
                defaultWidth = Math.max(baseWidth, 300);
            } else if (columnName.includes('respuesta')) {
                defaultWidth = Math.max(baseWidth, 200);
            } else if (columnName.includes('fuentes')) {
                defaultWidth = Math.max(baseWidth, 250);
            } else if (columnName.includes('notas')) {
                defaultWidth = Math.max(baseWidth, 400);
            } else if (columnName.includes('foto') || columnName.includes('imagen')) {
                defaultWidth = Math.max(baseWidth, 100);
            } else if (columnName.includes('nombre') || columnName.includes('lugar')) {
                defaultWidth = Math.max(baseWidth, 180);
            } else if (columnName.includes('ocupación') || columnName.includes('ocupacion')) {
                defaultWidth = Math.max(baseWidth, 150);
            } else if (columnName.includes('redes') || columnName.includes('rr ss')) {
                defaultWidth = Math.max(baseWidth, 120);
            } else {
                // Para columnas no específicas, usar el ancho del texto como mínimo
                defaultWidth = baseWidth;
            }
            
            // Aplicar el ancho
            th.style.width = defaultWidth + 'px';
            th.style.minWidth = defaultWidth + 'px';
            // No establecer maxWidth para permitir expansión ilimitada
        });
    }
    
    /**
     * Crea los handles de redimensionamiento
     */
    createResizeHandles() {
        const thead = this.table.querySelector('thead tr');
        if (!thead) return;
        
        const columns = thead.querySelectorAll('th');
        columns.forEach((th, index) => {
            // No crear handle para la última columna (acciones)
            if (index === columns.length - 1) return;
            
            const handle = document.createElement('div');
            handle.className = 'resize-handle';
            handle.style.cssText = `
                position: absolute;
                top: 0;
                right: -15px;
                width: 30px;
                height: 100%;
                background: transparent;
                cursor: col-resize;
                z-index: 10;
                user-select: none;
                border-right: 3px solid transparent;
                transition: border-color 0.2s ease;
            `;
            
            th.style.position = 'relative';
            th.appendChild(handle);
        });
    }
    
    /**
     * Vincula los eventos de redimensionamiento
     */
    bindEvents() {
        const handles = this.table.querySelectorAll('.resize-handle');
        
        handles.forEach(handle => {
            let isResizing = false;
            let startX = 0;
            let startWidth = 0;
            let currentColumn = null;
            let nextColumn = null;
            
            handle.addEventListener('mousedown', (e) => {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation(); // Evitar que otros event listeners se ejecuten
                
                isResizing = true;
                startX = e.clientX;
                currentColumn = handle.parentElement;
                nextColumn = currentColumn.nextElementSibling;
                startWidth = currentColumn.offsetWidth;
                
                // Prevenir selección de texto
                document.body.style.userSelect = 'none';
                document.body.style.cursor = 'col-resize';
                
                // Marcar que se está redimensionando
                this.table.setAttribute('data-resizing', 'true');
                this.table.classList.add('resizing');
                handle.classList.add('resizing');
                
                // Prevenir eventos de click en el header
                currentColumn.style.pointerEvents = 'none';
                
                console.log('🔧 [RESIZER] Iniciando redimensionamiento');
            });
            
            document.addEventListener('mousemove', (e) => {
                if (!isResizing) return;
                
                const deltaX = e.clientX - startX;
                const newWidth = Math.max(
                    this.options.minWidth,
                    startWidth + deltaX
                );
                
                // Actualizar ancho de la columna actual
                currentColumn.style.width = newWidth + 'px';
                currentColumn.style.minWidth = newWidth + 'px';
                // No establecer maxWidth para permitir expansión ilimitada
                
                // Si hay una columna siguiente, ajustar su ancho para mantener el ancho total
                if (nextColumn && nextColumn.tagName === 'TH') {
                    const nextNewWidth = Math.max(
                        this.options.minWidth,
                        nextColumn.offsetWidth - deltaX
                    );
                    nextColumn.style.width = nextNewWidth + 'px';
                    nextColumn.style.minWidth = nextNewWidth + 'px';
                    // No establecer maxWidth para permitir expansión ilimitada
                }
            });
            
            document.addEventListener('mouseup', () => {
                if (!isResizing) return;
                
                isResizing = false;
                document.body.style.userSelect = '';
                document.body.style.cursor = '';
                
                // Resetear todos los indicadores
                this.table.setAttribute('data-resizing', 'false');
                this.table.classList.remove('resizing');
                handle.classList.remove('resizing');
                
                // Restaurar eventos de click en el header
                if (currentColumn) {
                    currentColumn.style.pointerEvents = '';
                }
                
                console.log('🔧 [RESIZER] Finalizando redimensionamiento');
                
                // Guardar configuración en localStorage
                this.saveColumnWidths();
            });
        });
    }
    
    /**
     * Guarda los anchos de las columnas en localStorage
     */
    saveColumnWidths() {
        const thead = this.table.querySelector('thead tr');
        if (!thead) return;
        
        const columnWidths = {};
        const columns = thead.querySelectorAll('th');
        
        columns.forEach((th, index) => {
            const columnName = th.textContent.trim();
            columnWidths[columnName] = th.offsetWidth;
        });
        
        const tableId = this.table.id;
        localStorage.setItem(`table_${tableId}_column_widths`, JSON.stringify(columnWidths));
    }
    
    /**
     * Carga los anchos de las columnas desde localStorage
     */
    loadColumnWidths() {
        const tableId = this.table.id;
        const savedWidths = localStorage.getItem(`table_${tableId}_column_widths`);
        
        if (!savedWidths) return;
        
        try {
            const columnWidths = JSON.parse(savedWidths);
            const thead = this.table.querySelector('thead tr');
            if (!thead) return;
            
            const columns = thead.querySelectorAll('th');
            columns.forEach((th, index) => {
                const columnName = th.textContent.trim();
                if (columnWidths[columnName]) {
                    const width = columnWidths[columnName];
                    th.style.width = width + 'px';
                    th.style.minWidth = width + 'px';
                    // No establecer maxWidth para permitir expansión ilimitada
                }
            });
        } catch (error) {
            console.error('Error al cargar anchos de columnas:', error);
        }
    }
    
    /**
     * Resetea los anchos de las columnas a los valores por defecto
     */
    resetColumnWidths() {
        const tableId = this.table.id;
        localStorage.removeItem(`table_${tableId}_column_widths`);
        this.setDefaultColumnWidths();
    }
    
    /**
     * Ajusta automáticamente el ancho de las columnas según su contenido
     */
    autoResizeColumns() {
        const thead = this.table.querySelector('thead tr');
        if (!thead) return;
        
        const columns = thead.querySelectorAll('th');
        columns.forEach((th, index) => {
            // Calcular ancho basado en el contenido del encabezado
            const headerText = th.textContent.trim();
            const textWidth = headerText.length * 8; // Aproximadamente 8px por carácter
            const minWidth = Math.max(textWidth, this.options.minWidth);
            
            th.style.width = minWidth + 'px';
            th.style.minWidth = minWidth + 'px';
            // No establecer maxWidth para permitir expansión ilimitada
        });
    }
    
    /**
     * Verifica si se está redimensionando una columna
     */
    isResizing() {
        return this.table.getAttribute('data-resizing') === 'true';
    }
}

// Función de utilidad para inicializar el redimensionamiento
function initTableResizer(tableId, options = {}) {
    return new TableResizer(tableId, options);
}

// Auto-inicialización para tablas comunes
document.addEventListener('DOMContentLoaded', function() {
    // Inicializar para tabla de preguntas
    const preguntasTable = document.getElementById('tabla-preguntas');
    if (preguntasTable) {
        const resizer = new TableResizer('tabla-preguntas', {
            minWidth: 50,
            maxWidth: 1000
        });
        resizer.loadColumnWidths();
    }
    
    // Inicializar para tabla de concursantes
    const concursantesTable = document.getElementById('tabla-concursantes-principal');
    if (concursantesTable) {
        const resizer = new TableResizer('tabla-concursantes-principal', {
            minWidth: 50,
            maxWidth: 1000
        });
        resizer.loadColumnWidths();
    }
});

// Función global para verificar si se está redimensionando
function isTableResizing(tableId) {
    const table = document.getElementById(tableId);
    if (!table) return false;
    
    // Verificar múltiples indicadores
    const isResizing = table.getAttribute('data-resizing') === 'true';
    const hasResizingClass = table.classList.contains('resizing');
    const hasResizingHandle = table.querySelector('.resize-handle.resizing');
    
    return isResizing || hasResizingClass || hasResizingHandle !== null;
}

// Exportar para uso global
window.TableResizer = TableResizer;
window.initTableResizer = initTableResizer;
window.isTableResizing = isTableResizing;
