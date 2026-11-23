/**
 * Bloqueo de edición exclusiva con temporizador de 120s.
 */
const EditLockManager = {
    active: null,
    countdownId: null,
    renewId: null,
    timerEl: null,
    _expiring: false,
    _guardandoFuera: false,

    async parseErrorResponse(response) {
        const text = await response.text();
        if (typeof Utils !== 'undefined' && Utils.mensajeErrorHttp) {
            const detalle = Utils.extraerDetalleErrorCuerpo(text);
            return Utils.mensajeErrorHttp(response.status, detalle, 'editar este elemento');
        }
        return 'Otro usuario está editando este elemento. Inténtalo más tarde.';
    },

    async tryAcquire(entityType, entityId) {
        const response = await fetch('/api/edit-locks/acquire', {
            method: 'POST',
            headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ entityType, entityId: Number(entityId) })
        });
        if (response.status === 423 || response.status === 409) {
            throw new Error(await this.parseErrorResponse(response));
        }
        if (!response.ok) {
            throw new Error(await this.parseErrorResponse(response));
        }
        return response.json();
    },

    async renew(entityType, entityId) {
        const response = await fetch('/api/edit-locks/renew', {
            method: 'POST',
            headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ entityType, entityId: Number(entityId) })
        });
        if (!response.ok) return null;
        return response.json();
    },

    async release(entityType, entityId) {
        if (!entityType || entityId == null) return;
        try {
            await fetch('/api/edit-locks/release', {
                method: 'POST',
                headers: { ...authManager.getAuthHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ entityType, entityId: Number(entityId) })
            });
        } catch (e) {
            console.debug('[EditLock] Error liberando bloqueo:', e);
        }
    },

    hideModal(modalSelector) {
        const el = document.querySelector(modalSelector);
        if (!el) return;
        const inst = bootstrap.Modal.getInstance(el);
        if (inst) {
            inst.hide();
            return;
        }
        if (typeof $ !== 'undefined' && $(modalSelector).modal) {
            $(modalSelector).modal('hide');
        }
    },

    ensureTimerElement(modalEl) {
        if (!modalEl) return null;
        const header = modalEl.querySelector('.modal-header');
        if (!header) return null;
        let el = header.querySelector('.edit-lock-timer');
        if (!el) {
            el = document.createElement('div');
            el.className = 'edit-lock-timer ms-auto me-2 badge bg-warning text-dark';
            el.style.fontSize = '0.95rem';
            header.appendChild(el);
        }
        return el;
    },

    limpiarCapasModal() {
        document.querySelectorAll('.modal-backdrop').forEach((nodo) => nodo.remove());
        document.body.classList.remove('modal-open');
        document.body.style.removeProperty('padding-right');
        document.body.style.removeProperty('overflow');
    },

    modalVisibleSuperior() {
        const abiertos = Array.from(document.querySelectorAll('.modal.show'));
        if (!abiertos.length) return null;
        return abiertos.sort((a, b) => {
            const za = parseInt(window.getComputedStyle(a).zIndex, 10) || 0;
            const zb = parseInt(window.getComputedStyle(b).zIndex, 10) || 0;
            return za - zb;
        })[abiertos.length - 1];
    },

    accionDeGuardado(modal) {
        const id = modal && modal.id;
        if (id === 'modal-pregunta' && typeof PreguntasManager !== 'undefined') {
            return () => {
                const form = document.getElementById('formCrearPregunta');
                if (!form || !form.reportValidity()) return;
                return PreguntasManager.crearPregunta({ preventDefault() {}, target: form });
            };
        }
        if (id === 'modal-cuestionario' && typeof guardarCuestionario === 'function') return () => guardarCuestionario();
        if (id === 'modal-combo' && typeof guardarCombo === 'function') return () => guardarCombo();
        if (id === 'modalJornada' && typeof JornadasManager !== 'undefined') return () => JornadasManager.guardarJornada();
        if (id === 'modal-concursante' && typeof guardarConcursante === 'function') return () => guardarConcursante();
        if (id === 'modal-programa' && typeof guardarPrograma === 'function') return () => guardarPrograma();
        if (id === 'modal-repetido' && typeof guardarRepetido === 'function') return () => guardarRepetido();
        if (id === 'modal-usuario' && typeof procesarCreacionUsuario === 'function') {
            return () => {
                const form = document.getElementById('formCrearUsuario');
                if (!form || !form.reportValidity()) return;
                return procesarCreacionUsuario();
            };
        }
        return null;
    },

    asegurarCierrePorFuera() {
        if (this._fueraBound) return;
        this._fueraBound = true;
        document.addEventListener('click', (evento) => {
            const objetivo = evento.target;
            if (!objetivo || !objetivo.classList) return;
            const esFondo = objetivo.classList.contains('modal-backdrop');
            const esModal = objetivo.classList.contains('modal') && objetivo.classList.contains('show');
            if (!esFondo && !esModal) return;

            const abierto = esModal ? objetivo : this.modalVisibleSuperior();
            if (!abierto) {
                this.limpiarCapasModal();
                return;
            }

            const guardar = this.accionDeGuardado(abierto);
            if (guardar) {
                evento.preventDefault();
                evento.stopPropagation();
                if (this._guardandoFuera) return;
                this._guardandoFuera = true;
                Promise.resolve()
                    .then(() => guardar())
                    .catch((e) => console.error('[EditLock] Error guardando al clicar fuera:', e))
                    .finally(() => { this._guardandoFuera = false; });
                return;
            }

            const inst = bootstrap.Modal.getInstance(abierto) || bootstrap.Modal.getOrCreateInstance(abierto);
            inst.hide();
        }, true);
        document.addEventListener('hidden.bs.modal', () => {
            window.setTimeout(() => {
                if (!document.querySelector('.modal.show')) this.limpiarCapasModal();
            }, 50);
        });
    },

    pararTemporizadores() {
        if (this.countdownId) {
            clearInterval(this.countdownId);
            this.countdownId = null;
        }
        if (this.renewId) {
            clearInterval(this.renewId);
            this.renewId = null;
        }
        if (this.timerEl) {
            this.timerEl.remove();
            this.timerEl = null;
        }
    },

    startSession({ entityType, entityId, modalSelector, onExpire, ttlSeconds = 120 }) {
        this.asegurarCierrePorFuera();
        const previo = this.active;
        this.pararTemporizadores();
        if (previo && (previo.entityType !== entityType || Number(previo.entityId) !== Number(entityId))) {
            this.release(previo.entityType, previo.entityId);
        }
        this.active = {
            entityType,
            entityId: Number(entityId),
            modalSelector,
            onExpire,
            remaining: ttlSeconds,
            ttlSeconds
        };

        const modalEl = document.querySelector(modalSelector);
        this.timerEl = this.ensureTimerElement(modalEl);
        this.renderTimer();

        this.countdownId = setInterval(() => {
            if (!this.active) return;
            this.active.remaining -= 1;
            this.renderTimer();
            if (this.active.remaining <= 0) {
                this.handleExpire();
            }
        }, 1000);

        this.renewId = setInterval(async () => {
            if (!this.active) return;
            const data = await this.renew(this.active.entityType, this.active.entityId);
            if (!data && this.active) {
                this.active.remaining = 0;
            }
        }, 45000);

        if (modalEl && !modalEl._editLockBound) {
            modalEl._editLockBound = true;
            modalEl.addEventListener('hide.bs.modal', () => {
                if (this._expiring) return;
                if (this.active && document.querySelector(this.active.modalSelector) === modalEl) {
                    this.stopSession(true);
                }
            });
        }

        if (!this._beforeUnloadBound) {
            this._beforeUnloadBound = true;
            window.addEventListener('beforeunload', () => {
                if (!this.active) return;
                const payload = JSON.stringify({
                    entityType: this.active.entityType,
                    entityId: this.active.entityId
                });
                const headers = authManager?.getAuthHeaders?.() || {};
                fetch('/api/edit-locks/release', {
                    method: 'POST',
                    headers: { ...headers, 'Content-Type': 'application/json' },
                    body: payload,
                    keepalive: true
                }).catch(() => {});
            });
        }
    },

    renderTimer() {
        if (!this.timerEl || !this.active) return;
        const m = Math.floor(this.active.remaining / 60);
        const s = this.active.remaining % 60;
        this.timerEl.textContent = `Tiempo de edición: ${m}:${String(s).padStart(2, '0')}`;
        if (this.active.remaining <= 15) {
            this.timerEl.classList.remove('bg-warning');
            this.timerEl.classList.add('bg-danger', 'text-white');
        }
    },

    async handleExpire() {
        const session = this.active;
        if (!session || this._expiring) return;
        this._expiring = true;
        Toastify({
            text: 'Tiempo de edición agotado. Guardando cambios…',
            duration: 4000,
            close: true,
            gravity: 'top',
            position: 'right',
            style: { background: 'linear-gradient(to right, #ffc107, #ff9800)' }
        }).showToast();
        try {
            if (typeof session.onExpire === 'function') {
                await session.onExpire();
            }
        } catch (e) {
            console.error('[EditLock] Error al guardar al expirar:', e);
        }
        this.hideModal(session.modalSelector);
        await this.stopSession(true);
        this._expiring = false;
    },

    async stopSession(releaseLock = true) {
        this.pararTemporizadores();
        if (releaseLock && this.active) {
            await this.release(this.active.entityType, this.active.entityId);
        }
        this.active = null;
    },

    async openForEdit({ entityType, entityId, modalSelector, onExpire, openModalFn }) {
        await this.tryAcquire(entityType, entityId);
        if (typeof openModalFn === 'function') {
            await openModalFn();
        }
        this.startSession({ entityType, entityId, modalSelector, onExpire });
    }
};

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => EditLockManager.asegurarCierrePorFuera());
} else {
    EditLockManager.asegurarCierrePorFuera();
}
