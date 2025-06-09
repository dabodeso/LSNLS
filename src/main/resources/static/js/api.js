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

            const response = await fetch(this.baseUrl + endpoint, requestOptions);
            const endTime = performance.now();
            Logger.debug(`⏱️ Petición completada en ${Math.round(endTime - startTime)}ms`);

            // Manejar respuesta
            if (!response.ok) {
                if (response.status === 401) {
                    // Redirigir a login si no autorizado
                    window.location.href = 'login.html';
                    return; // Detener ejecución
                }
                const errorText = await response.text();
                Logger.error(`❌ ${options.method || 'GET'} ${endpoint} - Error ${response.status}:`, {
                    error: errorText,
                    headers: Object.fromEntries(response.headers.entries())
                });
                throw new Error(`${response.status}: ${errorText}`);
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