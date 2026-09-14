/**
 * Sistema de redimensionamiento de columnas para tablas
 * Permite redimensionar columnas arrastrando los bordes de los encabezados
 */

// Anchos pedidos por columna, en px, compartidos por todas las tablas que usan
// la misma clave de almacenamiento. En Programas conviven varias instancias
// (cabecera y cuerpo de cada programa) sobre las mismas columnas: si cada una
// guardase su propia copia, la última en persistir pisaría el arrastre recién
// hecho con los anchos viejos y la columna volvería a su sitio al repintar.
const anchosPorClave = new Map();

class TableResizer {
    constructor(tableId, options = {}) {
        this.table = document.getElementById(tableId);
        this.options = {
            minWidth: 50,
            // Ancho máximo razonable para evitar columnas gigantes
            maxWidth: 600,
            resizeHandleWidth: 4,
            ...options
        };
        this._resizeObserver = null;
        this._autoSaveTimer = null;
        
        if (!this.table) {
            console.error(`Tabla con ID '${tableId}' no encontrada`);
            return;
        }

        this.usaColgroup = !!this.table.querySelector('colgroup col');

        // Con table-layout: fixed el navegador reparte el sobrante, así que lo
        // medido no siempre es lo pedido y hay que recordar lo pedido.
        this.claveAnchos = this.getStorageKey();
        if (!anchosPorClave.has(this.claveAnchos)) {
            anchosPorClave.set(this.claveAnchos, []);
        }
        this.anchos = anchosPorClave.get(this.claveAnchos);
        
        this.init();
    }

    /**
     * Tablas que comparten anchos: las que llevan el mismo data-resizer-key.
     * En Programas la cabecera y el cuerpo son dos tablas distintas, y además
     * todos los programas muestran las mismas columnas.
     */
    getLinkedTables() {
        const key = (this.table.dataset || {}).resizerKey;
        if (!key) return [this.table];
        const escapada = (window.CSS && CSS.escape) ? CSS.escape(key) : key;
        const tablas = Array.from(document.querySelectorAll(`table[data-resizer-key="${escapada}"]`));
        return tablas.length ? tablas : [this.table];
    }

    /**
     * Aplica un ancho a la columna en todas las tablas enlazadas. Con
     * table-layout: fixed manda el <col>, no el <th>, así que hay que tocar ambos.
     */
    applyColumnWidth(index, width) {
        this.anchos[index] = width;
        this.getLinkedTables().forEach((tabla) => {
            const th = tabla.querySelectorAll('thead tr th')[index];
            if (th) {
                th.style.width = width + 'px';
                th.style.minWidth = width + 'px';
            }
            const col = tabla.querySelectorAll('colgroup col')[index];
            if (col) {
                col.style.width = width + 'px';
            }
        });
    }

    /**
     * Fija el ancho total de las tablas enlazadas a la suma de sus columnas.
     * Sin esto, el min-width del CSS reparte el hueco sobrante y las columnas
     * no respetan lo que ha pedido el usuario.
     */
    syncAnchoTabla() {
        if (!this.usaColgroup) return;
        const thead = this.table.querySelector('thead tr');
        if (!thead) return;
        const total = Array.from(thead.querySelectorAll('th')).reduce((suma, th, index) => {
            const ancho = this.anchos[index];
            return suma + (typeof ancho === 'number' && ancho > 0 ? ancho : Math.round(th.getBoundingClientRect().width));
        }, 0);
        if (!total) return;
        this.getLinkedTables().forEach((tabla) => {
            tabla.style.width = total + 'px';
            tabla.style.minWidth = total + 'px';
        });
    }
    
    init() {
        this.createResizeHandles();
        this.bindEvents();
        // Primero intentar cargar anchos guardados; si no hay, aplicar defaults
        const loaded = this.loadColumnWidths();
        if (!loaded) {
            this.setDefaultColumnWidths();
        }
        // Guardado robusto: si el ancho cambia por CSS/JS/resize nativo, persistirlo igual
        this.setupAutoSaveOnResize();
    }

    /**
     * Observa cambios reales de tamaño en <th> y persiste automáticamente.
     * Esto cubre casos donde el usuario cambia el ancho sin usar nuestros handles
     * (por ejemplo, por CSS `resize: horizontal` o por scripts externos).
     */
    setupAutoSaveOnResize() {
        try {
            if (typeof ResizeObserver === 'undefined') return;
            const thead = this.table.querySelector('thead tr');
            if (!thead) return;

            const columns = thead.querySelectorAll('th');
            if (!columns || columns.length === 0) return;

            // Desconectar si ya existía
            if (this._resizeObserver) {
                try { this._resizeObserver.disconnect(); } catch (e) { /* ignore */ }
            }

            const scheduleSave = () => {
                if (this._autoSaveTimer) clearTimeout(this._autoSaveTimer);
                this._autoSaveTimer = setTimeout(() => {
                    this._autoSaveTimer = null;
                    this.saveColumnWidths();
                }, 150);
            };

            this._resizeObserver = new ResizeObserver(() => {
                // Si la tabla ya no está en DOM, desconectar
                if (!document.body || !document.body.contains(this.table)) {
                    try { this._resizeObserver.disconnect(); } catch (e) { /* ignore */ }
                    this._resizeObserver = null;
                    return;
                }
                scheduleSave();
            });

            columns.forEach((th) => {
                try { this._resizeObserver.observe(th); } catch (e) { /* ignore */ }
            });
        } catch (e) {
            // ignore
        }
    }

    /**
     * Determina una clave de vista estable para separar preferencias
     * entre "vista actual", "vista 2", "vista 3", etc.
     */
    getViewKey() {
        try {
            const body = document.body;
            const path = (globalThis?.location?.pathname) ? globalThis.location.pathname : '';
            // Jornadas: j2 / j3
            if (body && body.classList.contains('j3-mode')) return 'j3';
            if (body && body.classList.contains('j2-mode')) return 'j2';
            // Cuestionarios: q2
            if (body && body.classList.contains('q2-mode')) return 'q2';
            // Combos: c2
            if (body && body.classList.contains('c2-mode')) return 'c2';

            // Importante: NO mezclar claves de vistas de otras pantallas.
            // Solo usar estas preferencias si estamos en la ruta correspondiente.
            if (path.includes('jornadas')) {
                const vJ = localStorage.getItem('vistaJornadas');
                if (vJ) return vJ;
            }
            if (path.includes('cuestionarios')) {
                const vQ = localStorage.getItem('vistaCuestionarios');
                if (vQ) return vQ;
            }
            if (path.includes('combos')) {
                const vC = localStorage.getItem('vistaCombos');
                if (vC) return vC;
            }
        } catch (e) {
            // ignore
        }
        return 'default';
    }

    /**
     * Clave única para almacenar/restaurar anchos de columnas
     * separando por ruta + tabla + vista.
     */
    getStorageKey() {
        const tableId = this.table.id;
        const resizerKey = (this.table && this.table.dataset) ? this.table.dataset.resizerKey : null;
        const tableKey = (resizerKey && String(resizerKey).trim()) ? String(resizerKey).trim() : tableId;
        const path = (window && window.location && window.location.pathname) ? window.location.pathname : '';
        const view = this.getViewKey();
        const version = path.includes('programas') ? 'column_widths_v4' : 'column_widths_v2';
        return `table_${path}__${tableKey}__${view}__${version}`;
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
            const isProgramaConcursantes = (this.table.id || '').includes('programa')
                && (this.table.id || '').includes('concursantes');
            if (isProgramaConcursantes) {
                const anchosPrograma = {
                    'nº conc': 50,
                    'lugar': 100,
                    'nombre': 140,
                    'edad': 50,
                    'ocupación': 120,
                    'rr ss': 100,
                    'resultado': 70,
                    'dur conc': 70,
                    'foto': 240,
                    'mom. destacados': 260,
                    'xusóker': 110,
                    'x': 50,
                    'val': 70,
                    'acc': 50
                };
                if (anchosPrograma[columnName]) {
                    this.applyColumnWidth(index, anchosPrograma[columnName]);
                    return;
                }
            }
            
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
                // Doble ancho por defecto para "NOTAS GRABACIÓN"
                defaultWidth = Math.max(baseWidth, 800);
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
            this.applyColumnWidth(index, defaultWidth);
            // No establecer maxWidth para permitir expansión ilimitada
        });
        this.syncAnchoTabla();
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
            
            // El tirador va dentro de su propio <th>. Si sobresale hacia el
            // siguiente, la parte que asoma queda tapada: las cabeceras llevan
            // z-index en el CSS, y la de la derecha gana por ir después.
            const handle = document.createElement('div');
            handle.className = 'resize-handle';
            handle.style.cssText = `
                position: absolute;
                top: 0;
                right: 0;
                width: 12px;
                height: 100%;
                background: transparent;
                cursor: col-resize;
                z-index: 20;
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
            let columnIndex = -1;
            
            handle.addEventListener('mousedown', (e) => {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation(); // Evitar que otros event listeners se ejecuten
                
                isResizing = true;
                startX = e.clientX;
                currentColumn = handle.parentElement;
                columnIndex = Array.prototype.indexOf.call(currentColumn.parentElement.children, currentColumn);
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
                let newWidth = startWidth + deltaX;
                // Aplicar límites mínimo y máximo
                newWidth = Math.max(this.options.minWidth, newWidth);
                if (this.options.maxWidth && typeof this.options.maxWidth === 'number') {
                    newWidth = Math.min(this.options.maxWidth, newWidth);
                }
                
                // Actualizar ancho de la columna actual
                this.applyColumnWidth(columnIndex, newWidth);
                this.syncAnchoTabla();
                // No establecer maxWidth para permitir expansión ilimitada
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
        
        const columns = thead.querySelectorAll('th');
        const widths = [];
        columns.forEach((th, index) => {
            const pedido = this.anchos[index];
            if (this.usaColgroup && typeof pedido === 'number' && pedido > 0) {
                widths[index] = pedido;
                return;
            }
            const w = th.getBoundingClientRect ? th.getBoundingClientRect().width : th.offsetWidth;
            widths[index] = Math.round(w);
        });

        try {
            localStorage.setItem(this.getStorageKey(), JSON.stringify({ v: 2, widths }));
        } catch (e) {
            console.error('Error al guardar anchos de columnas:', e);
        }
    }
    
    /**
     * Carga los anchos de las columnas desde localStorage
     */
    loadColumnWidths() {
        const savedWidths = localStorage.getItem(this.getStorageKey());
        
        if (!savedWidths) return false;
        
        try {
            const parsed = JSON.parse(savedWidths);
            const thead = this.table.querySelector('thead tr');
            if (!thead) return false;
            
            const columns = thead.querySelectorAll('th');
            // Formato nuevo (v2): array por índice
            if (parsed && parsed.v === 2 && Array.isArray(parsed.widths)) {
                columns.forEach((th, index) => {
                    let width = parsed.widths[index];
                    if (typeof width === 'number' && width > 0) {
                        // Aplicar también el límite máximo configurado
                        if (this.options.maxWidth && typeof this.options.maxWidth === 'number') {
                            width = Math.min(this.options.maxWidth, width);
                        }
                        this.applyColumnWidth(index, width);
                    }
                });
                this.syncAnchoTabla();
                return true;
            }

            // Compatibilidad retro: formato anterior por nombre de columna
            if (parsed && typeof parsed === 'object') {
                columns.forEach((th, index) => {
                    const columnName = th.textContent.trim();
                    const width = parsed[columnName];
                    if (typeof width === 'number' && width > 0) {
                        this.applyColumnWidth(index, width);
                    }
                });
                this.syncAnchoTabla();
                return true;
            }
        } catch (error) {
            console.error('Error al cargar anchos de columnas:', error);
        }
        return false;
    }
    
    /**
     * Resetea los anchos de las columnas a los valores por defecto
     */
    resetColumnWidths() {
        try {
            localStorage.removeItem(this.getStorageKey());
        } catch (e) {
            // ignore
        }
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

function _autoInitTableResizers() {
    try {
        const tables = document.querySelectorAll('table.table-excel[id], table[id].table-excel, table[id][data-resizable="true"]');
        tables.forEach((t) => {
            const id = t.id;
            if (!id) return;
            // Evitar doble init (re-render de tablas dinámicas)
            if (t.dataset && t.dataset.resizerInit === '1') return;
            if (t.querySelector('.resize-handle')) return;
            // Marcar antes para evitar bucles si el init muta el DOM
            if (t.dataset) t.dataset.resizerInit = '1';
            new TableResizer(id, { minWidth: 50, maxWidth: 1000 });
        });
    } catch (e) {
        console.error('Error al auto-inicializar TableResizer:', e);
    }
}

// Auto-inicialización
document.addEventListener('DOMContentLoaded', function() {
    _autoInitTableResizers();

    // Observa inserciones dinámicas (Programas/Combos/Cuestionarios renderizan tablas tras cargar datos)
    try {
        const obs = new MutationObserver(() => {
            // Throttle simple: agrupar mutaciones y ejecutar una vez por tick
            if (window.__tableResizerAutoInitPending) return;
            window.__tableResizerAutoInitPending = true;
            setTimeout(() => {
                window.__tableResizerAutoInitPending = false;
                _autoInitTableResizers();
            }, 0);
        });
        obs.observe(document.body, { childList: true, subtree: true });
        window.__tableResizerObserver = obs;
    } catch (e) {
        // ignore
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
window.autoInitTableResizers = _autoInitTableResizers;
