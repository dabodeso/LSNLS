/**
 * Monitor de cambios en entidades visibles (polling cada 30s).
 */
const SyncMonitor = {
    items: new Map(),
    intervalId: null,
    pollMs: 30000,
    notifiedKeys: new Set(),

    key(type, id) {
        return `${type}:${id}`;
    },

    register(type, id, version, label) {
        if (!type || id == null) return;
        this.items.set(this.key(type, id), {
            entityType: type,
            entityId: Number(id),
            version: version != null ? Number(version) : 0,
            label: label || `${type} ${id}`
        });
        this.start();
    },

    registerMany(entries) {
        if (!Array.isArray(entries)) return;
        entries.forEach(e => this.register(e.entityType, e.entityId, e.version, e.label));
    },

    updateVersion(type, id, version) {
        const k = this.key(type, id);
        if (this.items.has(k)) {
            const item = this.items.get(k);
            item.version = version != null ? Number(version) : item.version;
            this.notifiedKeys.delete(k);
        }
    },

    clear() {
        this.items.clear();
        this.notifiedKeys.clear();
        this.stop();
    },

    resetFromVisible(entries) {
        this.items.clear();
        this.notifiedKeys.clear();
        this.registerMany(entries);
    },

    start() {
        if (this.intervalId || this.items.size === 0) return;
        this.intervalId = setInterval(() => this.check(), this.pollMs);
    },

    stop() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
    },

    async check() {
        if (!authManager?.isAuthenticated?.() || this.items.size === 0) return;
        try {
            const payload = Array.from(this.items.values()).map(i => ({
                entityType: i.entityType,
                entityId: i.entityId,
                version: i.version
            }));
            const response = await fetch('/api/sync/visible-changes', {
                method: 'POST',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) return;
            const data = await response.json();
            const changes = data.changes || [];
            changes.forEach(change => this.notifyChange(change));
        } catch (e) {
            console.debug('[SyncMonitor] Error comprobando cambios:', e);
        }
    },

    notifyChange(change) {
        const k = this.key(change.entityType, change.entityId);
        if (this.notifiedKeys.has(k)) return;
        this.notifiedKeys.add(k);

        const mensaje = change.mensaje || `${change.usuarioNombre || 'Otro usuario'} ha actualizado ${change.entityLabel || change.entityId}. Refresca para ver los cambios.`;

        Toastify({
            text: mensaje,
            duration: 12000,
            close: true,
            gravity: 'top',
            position: 'center',
            style: { background: 'linear-gradient(to right, #ff9800, #f57c00)', maxWidth: '520px' },
            onClick: () => {
                this.refreshEntity(change.entityType, change.entityId, change.version);
            }
        }).showToast();
    },

    async refreshEntity(type, id, newVersion) {
        const handlers = {
            PREGUNTA: () => window.PreguntasManager?.recargarConFiltros?.(),
            COMBO: () => window.CombosManager?.recargarConFiltros?.(),
            CUESTIONARIO: () => window.CuestionariosManager?.recargarConFiltros?.(),
            JORNADA: () => window.JornadasManager?.recargarConFiltros?.(),
            PROGRAMA: () => typeof recargarProgramas === 'function' ? recargarProgramas() : null
        };
        const fn = handlers[type];
        if (typeof fn === 'function') {
            await fn();
        }
        if (newVersion != null) {
            this.updateVersion(type, id, newVersion);
        }
        this.notifiedKeys.delete(this.key(type, id));
    }
};

document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && SyncMonitor.items.size > 0) {
        SyncMonitor.check();
    }
});
