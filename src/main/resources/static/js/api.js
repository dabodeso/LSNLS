// Logger básico para evitar errores si no existe
const Logger = {
    debug: console.debug,
    info: console.info,
    warn: console.warn,
    error: console.error,
    success: console.log
};

// api.js - Gestión de API optimizada
class ApiManager {
    constructor() {
        this.baseUrl = '';  // URL base de la API
        // Id de la operación deshacible registrada por el backend en la última
        // petición mutadora (cabecera X-Undo-Operacion-Id). Permite deshacerla
        // con POST /api/undo/{id}.
        this.ultimaOperacionUndoId = null;
        console.log('🌐 ApiManager inicializado');
    }

    async makeRequest(endpoint, options = {}) {
        const startTime = performance.now();
        
        try {
            // Obtener token antes de la petición
            const token = authManager.getToken();
            if (!token) {
                Logger.warning('No hay token de autenticación disponible');
            }

            // Configurar headers
            const headers = new Headers({
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
                ...(options.headers || {})
            });

            // Configurar opciones completas
            const requestOptions = {
                ...options,
                headers
            };

            // Si hay body, asegurarse de que es JSON
            if (requestOptions.body && typeof requestOptions.body === 'string') {
                try {
                    JSON.parse(requestOptions.body);
                } catch (e) {
                    Logger.error('Error: El body no es un JSON válido', e);
                    throw new Error('El body debe ser un JSON válido');
                }
            }

            Logger.debug(`📡 ${options.method || 'GET'} ${endpoint}`, {
                headers: Object.fromEntries(headers.entries()),
                body: requestOptions.body
            });

            this.ultimaOperacionUndoId = null;
            const response = await fetch(this.baseUrl + endpoint, requestOptions);
            const endTime = performance.now();
            Logger.debug(`⏱️ Petición completada en ${Math.round(endTime - startTime)}ms`);

            if (response.ok) {
                const undoId = response.headers.get('X-Undo-Operacion-Id');
                this.ultimaOperacionUndoId = undoId ? Number(undoId) : null;
            }

            // Manejar respuesta
            if (!response.ok) {
                const errorText = await response.text();
                Logger.error(`❌ ${options.method || 'GET'} ${endpoint} - Error ${response.status}:`, {
                    error: errorText,
                    headers: Object.fromEntries(response.headers.entries())
                });
                
                if (response.status === 401) {
                    throw new Error('UNAUTHORIZED: Token expirado o inválido');
                }

                const detalle = typeof Utils !== 'undefined' && Utils.extraerDetalleErrorCuerpo
                    ? Utils.extraerDetalleErrorCuerpo(errorText)
                    : errorText.trim();
                const mensaje = typeof Utils !== 'undefined' && Utils.mensajeErrorHttp
                    ? Utils.mensajeErrorHttp(response.status, detalle, options.errorAccion || 'completar la petición')
                    : (detalle || `Error ${response.status}`);
                throw new Error(mensaje);
            }

            // Si la respuesta está vacía, devolver null
            if (response.status === 204) {
                Logger.debug(`✅ ${options.method || 'GET'} ${endpoint} - Sin contenido`);
                return null;
            }

            // Si la respuesta no tiene body JSON, devolver null
            const contentType = response.headers.get('content-type');
            if (!contentType || contentType.indexOf('application/json') === -1) {
                Logger.debug(`✅ ${options.method || 'GET'} ${endpoint} - Sin JSON en respuesta`);
                return null;
            }

            const data = await response.json();
            Logger.debug(`✅ ${options.method || 'GET'} ${endpoint} - Respuesta:`, data);
            return data;

        } catch (error) {
            Logger.error(`❌ Error en petición ${endpoint}:`, error);
            throw error;
        }
    }

    async get(endpoint) {
        return this.makeRequest(endpoint);
    }

    async post(endpoint, data, options = {}) {
        return this.makeRequest(endpoint, {
            method: 'POST',
            headers: options.headers || {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data),
            ...options
        });
    }

    // POST multipart (p. ej. fotos). No fuerza Content-Type JSON para que el
    // navegador ponga el boundary. Captura X-Undo-Operacion-Id si el backend
    // registró la operación.
    async postMultipart(endpoint, formData) {
        const token = authManager.getToken();
        this.ultimaOperacionUndoId = null;
        const response = await fetch(this.baseUrl + endpoint, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            },
            body: formData
        });
        if (!response.ok) {
            if (response.status === 401) {
                throw new Error('UNAUTHORIZED: Token expirado o inválido');
            }
            throw new Error(await Utils.mensajeDesdeResponse(response, 'completar la petición'));
        }
        const undoId = response.headers.get('X-Undo-Operacion-Id');
        this.ultimaOperacionUndoId = undoId ? Number(undoId) : null;
        const contentType = response.headers.get('content-type');
        if (!contentType || contentType.indexOf('application/json') === -1) {
            return null;
        }
        return response.json();
    }

    async postFormDataUndoable(endpoint, formData, { label, redo } = {}) {
        const result = await this.postMultipart(endpoint, formData);
        this.registrarUndoBackend({ label, redo });
        return result;
    }

    // POST con undo respaldado por el backend (cabecera X-Undo-Operacion-Id).
    async postUndoableBackend(endpoint, data, { label, redo } = {}) {
        const result = await this.post(endpoint, data);
        this.registrarUndoBackend({
            label,
            redo: redo || (async () => { await this.post(endpoint, data); })
        });
        return result;
    }

    async put(endpoint, data, options = {}) {
        return this.makeRequest(endpoint, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                ...(options.headers || {})
            },
            body: JSON.stringify(data),
            ...options
        });
    }

    // ==== OPERACIONES DESHACIBLES (GENÉRICAS) ====

    // Prepara un payload de restauración con la versión optimista actual del
    // recurso, para que el PUT de undo no falle con 409 por versión antigua.
    async _conVersionActual(snapUrl, previous) {
        if (!previous || typeof previous !== 'object' || !('version' in previous)) {
            return previous;
        }
        try {
            const actual = await this.get(snapUrl);
            if (actual && typeof actual === 'object' && 'version' in actual) {
                return { ...previous, version: actual.version };
            }
        } catch (e) {
            console.warn('[Undoable] No se pudo refrescar la versión del recurso:', e);
        }
        return previous;
    }

    // PUT deshaciente: hace snapshot previo (GET) y registra Undo/Redo
    async putUndoable(endpoint, data, { label, snapshotEndpoint } = {}) {
        const snapUrl = snapshotEndpoint || endpoint;
        let previous = null;
        try {
            previous = await this.get(snapUrl);
        } catch (e) {
            console.warn('[Undoable PUT] No se pudo obtener snapshot previo:', e);
        }

        const result = await this.put(endpoint, data);

        if (window.UndoManager && previous) {
            const restoreUrl = snapshotEndpoint || endpoint;
            const hacer = async () => { await this.put(endpoint, data); };
            const deshacer = async () => {
                const restaurar = await this._conVersionActual(restoreUrl, previous);
                await this.put(restoreUrl, restaurar);
            };
            window.UndoManager.record({ do: hacer, undo: deshacer, label: label || `PUT ${endpoint}` });
        }

        return result;
    }

    // DELETE deshaciente respaldado por backend: si el servidor registró una
    // operación deshacible (cabecera X-Undo-Operacion-Id), el undo llama a
    // POST /api/undo/{id}, que restaura las filas borradas con su MISMO id
    // y los estados afectados. El redo repite el DELETE (válido porque el
    // undo recreó el recurso con el mismo id).
    async deleteUndoable(endpoint, { label } = {}) {
        const result = await this.delete(endpoint);

        let opId = this.ultimaOperacionUndoId;
        if (window.UndoManager && opId) {
            const deshacer = async () => { await this.post(`/api/undo/${opId}`, {}); };
            const rehacer = async () => {
                await this.delete(endpoint);
                // El nuevo DELETE registra otra operación; el siguiente undo debe usarla
                if (this.ultimaOperacionUndoId) opId = this.ultimaOperacionUndoId;
            };
            window.UndoManager.record({ do: rehacer, undo: deshacer, label: label || `DELETE ${endpoint}` });
        } else if (label) {
            console.info(`[Undoable DELETE] "${label}": el backend no registró undo; borrado definitivo`);
        }

        return result;
    }

    // Registra en el UndoManager la última operación deshacible del backend.
    // `redo` es opcional: función que re-ejecuta la operación original.
    registrarUndoBackend({ label, redo } = {}) {
        let opId = this.ultimaOperacionUndoId;
        if (!window.UndoManager || !opId) return false;
        const accion = {
            undo: async () => { await this.post(`/api/undo/${opId}`, {}); },
            label
        };
        if (typeof redo === 'function') {
            accion.do = async () => {
                await redo();
                if (this.ultimaOperacionUndoId) opId = this.ultimaOperacionUndoId;
            };
        }
        window.UndoManager.record(accion);
        return true;
    }

    // POST deshaciente (opcional): requiere cómo obtener el id creado
    async postUndoable(endpoint, data, { label, idExtractor, deleteEndpointBuilder } = {}) {
        // idExtractor: (response) => id o (obj) => obj.id
        // deleteEndpointBuilder: (id) => `/api/entidad/${id}`
        const created = await this.post(endpoint, data);
        try {
            if (window.UndoManager && idExtractor && deleteEndpointBuilder) {
                // El id se guarda en una variable mutable: al rehacer (nuevo POST)
                // el recurso recreado tiene otro id, y el siguiente undo debe
                // borrar ese id nuevo, no el original.
                let idActual = idExtractor(created);
                const rehacer = async () => {
                    const recreado = await this.post(endpoint, data);
                    idActual = idExtractor(recreado);
                };
                const deshacer = async () => { await this.delete(deleteEndpointBuilder(idActual)); };
                window.UndoManager.record({ do: rehacer, undo: deshacer, label: label || `POST ${endpoint}` });
            }
        } catch (e) {
            console.warn('[Undoable POST] No se pudo registrar undo:', e);
        }
        return created;
    }

    async patch(endpoint, data, options = {}) {
        return this.makeRequest(endpoint, {
            method: 'PATCH',
            headers: options.headers || {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data),
            ...options
        });
    }

    // PATCH deshaciente: snapshot previo (GET) y revierte solo los campos tocados
    async patchUndoable(endpoint, data, { label, snapshotEndpoint, onAfter, mapRevert } = {}) {
        if (!snapshotEndpoint) {
            console.warn('[Undoable PATCH] snapshotEndpoint requerido; ejecutando PATCH sin undo');
            return this.patch(endpoint, data);
        }

        let previous = null;
        try {
            previous = await this.get(snapshotEndpoint);
        } catch (e) {
            console.warn('[Undoable PATCH] No se pudo obtener snapshot previo:', e);
        }

        const result = await this.patch(endpoint, data);

        if (onAfter) {
            await onAfter(data, previous, 'initial');
        }

        if (window.UndoManager && previous) {
            const buildRevert = () => {
                const revert = {};
                for (const key of Object.keys(data)) {
                    const raw = previous[key] ?? null;
                    revert[key] = typeof mapRevert === 'function' ? mapRevert(key, raw, previous) : raw;
                }
                return revert;
            };

            const hacer = async () => {
                await this.patch(endpoint, data);
                if (onAfter) await onAfter(data, previous, 'do');
            };
            const deshacer = async () => {
                await this.patch(endpoint, buildRevert());
                if (onAfter) await onAfter(buildRevert(), previous, 'undo');
            };
            window.UndoManager.record({ do: hacer, undo: deshacer, label: label || `PATCH ${endpoint}` });
        }

        return result;
    }

    async delete(endpoint, options = {}) {
        return this.makeRequest(endpoint, {
            method: 'DELETE',
            ...options
        });
    }

    // === PREGUNTAS ===
    async getPreguntas() {
        const response = await this.makeRequest('/api/preguntas');
        return response.ok ? await response.json() : [];
    }

    async getPreguntaById(id) {
        const response = await this.makeRequest(`/api/preguntas/${id}`);
        return response.ok ? await response.json() : null;
    }

    async createPregunta(pregunta) {
        const response = await this.makeRequest('/api/preguntas', {
            method: 'POST',
            body: JSON.stringify(pregunta)
        });
        return response;
    }

    async updatePregunta(id, pregunta) {
        const response = await this.makeRequest(`/api/preguntas/${id}`, {
            method: 'PUT',
            body: JSON.stringify(pregunta)
        });
        return response;
    }

    async deletePregunta(id) {
        const response = await this.makeRequest(`/api/preguntas/${id}`, {
            method: 'DELETE'
        });
        return response;
    }

    async getPreguntasDisponibles(nivel) {
        const response = await this.makeRequest(`/api/preguntas/disponibles/${nivel}`);
        return response.ok ? await response.json() : [];
    }

    async cambiarEstadoPregunta(id, nuevoEstado) {
        const response = await this.makeRequest(`/api/preguntas/${id}/estado?nuevoEstado=${nuevoEstado}`, {
            method: 'PUT'
        });
        return response;
    }

    // === CUESTIONARIOS ===
    async getCuestionarios() {
        const response = await this.makeRequest('/api/cuestionarios');
        if (response.ok) {
            const cuestionarios = await response.json();
            console.log('Cuestionarios obtenidos:', cuestionarios);
            return cuestionarios;
        }
        return [];
    }

    async getCuestionarioById(id) {
        const response = await this.makeRequest(`/api/cuestionarios/${id}`);
        if (response.ok) {
            const cuestionario = await response.json();
            console.log('Cuestionario obtenido:', cuestionario);
            return cuestionario;
        }
        return null;
    }

    async createCuestionario(cuestionario) {
        try {
            Logger.debug('Creando cuestionario:', cuestionario);
            
            const token = authManager.getToken();
            if (!token) {
                Logger.error('No hay token de autenticación');
                throw new Error('No hay token de autenticación');
            }

            Logger.debug('Token de autenticación:', token);

            const response = await this.makeRequest('/api/cuestionarios', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(cuestionario)
            });

            Logger.success('Cuestionario creado exitosamente:', response);
            return response;
        } catch (error) {
            Logger.error('Error creando cuestionario:', error);
            throw error;
        }
    }

    async updateCuestionario(id, cuestionario) {
        const response = await this.makeRequest(`/api/cuestionarios/${id}`, {
            method: 'PUT',
            body: JSON.stringify(cuestionario)
        });
        return response;
    }

    async deleteCuestionario(id) {
        const response = await this.makeRequest(`/api/cuestionarios/${id}`, {
            method: 'DELETE'
        });
        return response;
    }

    async agregarPreguntaACuestionario(cuestionarioId, preguntaId, factorMultiplicacion = 1) {
        const response = await this.makeRequest(`/api/cuestionarios/${cuestionarioId}/preguntas`, {
            method: 'POST',
            body: JSON.stringify({
                preguntaId: preguntaId,
                factorMultiplicacion: factorMultiplicacion
            })
        });
        return response;
    }

    async quitarPreguntaDeCuestionario(cuestionarioId, preguntaId) {
        const response = await this.makeRequest(`/api/cuestionarios/${cuestionarioId}/preguntas/${preguntaId}`, {
            method: 'DELETE'
        });
        return response;
    }

    // === COMBOS ===
    async getCombos() {
        try {
            Logger.debug('📥 Obteniendo lista de combos...');
            const response = await this.makeRequest('/api/combos');
            Logger.debug('Respuesta de combos:', response);
            return response || [];
        } catch (error) {
            Logger.error('Error obteniendo combos:', error);
            return [];
        }
    }

    async getComboById(id) {
        const response = await this.makeRequest(`/api/combos/${id}`);
        return response.ok ? await response.json() : null;
    }

    async createCombo(combo) {
        const response = await this.makeRequest('/api/combos', {
            method: 'POST',
            body: JSON.stringify(combo)
        });
        return response;
    }

    async updateCombo(id, combo) {
        const response = await this.makeRequest(`/api/combos/${id}`, {
            method: 'PUT',
            body: JSON.stringify(combo)
        });
        return response;
    }

    async deleteCombo(id) {
        const response = await this.makeRequest(`/api/combos/${id}`, {
            method: 'DELETE'
        });
        return response;
    }

    // === USUARIOS ===
    async getUsuarios() {
        return await this.makeRequest('/api/usuarios');
    }

    async createUsuario(usuario) {
        const response = await this.makeRequest('/api/usuarios', {
            method: 'POST',
            body: JSON.stringify(usuario)
        });
        return response;
    }

    async updateUsuario(id, usuario) {
        const response = await this.makeRequest(`/api/usuarios/${id}`, {
            method: 'PUT',
            body: JSON.stringify(usuario)
        });
        return response;
    }

    async deleteUsuario(id) {
        const response = await this.makeRequest(`/api/usuarios/${id}`, {
            method: 'DELETE'
        });
        return response;
    }

    async resetPasswordUsuario(id) {
        const response = await this.makeRequest(`/api/usuarios/${id}/reset-password`, {
            method: 'POST'
        });
        return response;
    }

    // === VALIDACIÓN Y TRANSFORMACIÓN ===
    async validarTexto(texto, tipo) {
        const response = await this.makeRequest('/api/preguntas/validar', {
            method: 'POST',
            body: JSON.stringify({ texto, tipo })
        });
        return response.ok ? await response.json() : null;
    }

    async transformarTexto(texto, tipo) {
        const response = await this.makeRequest('/api/preguntas/transformar', {
            method: 'POST',
            body: JSON.stringify({ texto, tipo })
        });
        return response.ok ? await response.json() : null;
    }
}

// Instancia global
const apiManager = new ApiManager(); 