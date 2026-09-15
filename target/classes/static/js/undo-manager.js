// Gestor global de deshacer/rehacer con atajos Ctrl+Z / Ctrl+Y
// API pública:
//   UndoManager.record({ do: fnDo, undo: fnUndo, label?: string })
//   UndoManager.undo()
//   UndoManager.redo()
//   UndoManager.clear()
//   UndoManager.canUndo() / canRedo()
//   UndoManager.enabled = true/false

(function () {
  // Campos donde el usuario escribe texto: el navegador gestiona su propio
  // Ctrl+Z/Ctrl+Y (deshacer texto), así que la app no debe interceptarlo.
  const isTextEntryElement = (el) => {
    if (!el) return false;
    if (el.isContentEditable) return true;
    const tag = el.tagName ? el.tagName.toLowerCase() : '';
    if (tag === 'textarea') return true;
    if (tag === 'input') {
      const type = (el.type || 'text').toLowerCase();
      return ['text', 'search', 'url', 'tel', 'email', 'password', 'number'].includes(type);
    }
    return false;
  };

  const valorBaseline = (el) => {
    if (!el) return '';
    if (el.type === 'checkbox' || el.type === 'radio') return el.checked ? 'true' : 'false';
    return el.value == null ? '' : String(el.value);
  };

  const marcarBaseline = (el) => {
    if (!el || !el.dataset) return;
    el.dataset.undoBaseline = valorBaseline(el);
  };

  const marcarBaselinesEn = (root) => {
    if (!root || !root.querySelectorAll) return;
    root.querySelectorAll('input, textarea, select').forEach(marcarBaseline);
  };

  const controlEstaSucio = (el) => {
    if (!el || el.dataset.undoBaseline === undefined) return false;
    return valorBaseline(el) !== el.dataset.undoBaseline;
  };

  // Restaura los campos del modal abierto a como estaban al abrirlo.
  // Así Ctrl+Z deshace un cambio de estado (o de otro select) sin haber guardado.
  const restaurarFormularioModalAbierto = () => {
    const modal = document.querySelector('.modal.show');
    if (!modal) return false;
    let restaurado = false;
    modal.querySelectorAll('select, input[type="checkbox"], input[type="radio"]').forEach((el) => {
      if (!controlEstaSucio(el)) return;
      if (el.type === 'checkbox' || el.type === 'radio') {
        el.checked = el.dataset.undoBaseline === 'true';
      } else {
        el.value = el.dataset.undoBaseline;
      }
      el.dispatchEvent(new Event('change', { bubbles: true }));
      restaurado = true;
    });
    return restaurado;
  };

  function notificarUndo(mensaje, esError = false) {
    if (typeof Toastify === 'function') {
      Toastify({
        text: mensaje,
        duration: 2500,
        close: true,
        gravity: 'top',
        position: 'right',
        style: {
          background: esError
            ? 'linear-gradient(to right, #ff0000, #cc0000)'
            : 'linear-gradient(to right, #00b09b, #96c93d)'
        }
      }).showToast();
    } else {
      console.log('[UndoManager]', mensaje);
    }
  }

  class Stack {
    constructor() {
      this.items = [];
    }
    push(item) {
      this.items.push(item);
    }
    pop() {
      return this.items.pop();
    }
    peek() {
      return this.items[this.items.length - 1];
    }
    clear() {
      this.items = [];
    }
    get length() {
      return this.items.length;
    }
  }

  class UndoManagerImpl {
    constructor() {
      this.undoStack = new Stack();
      this.redoStack = new Stack();
      this.enabled = true;
      this.maxEntries = 500;
      this._busy = false;
      this._installGlobalShortcuts();
    }

    canUndo() {
      return this.enabled && this.undoStack.length > 0;
    }

    canRedo() {
      return this.enabled && this.redoStack.length > 0;
    }

    record(action) {
      if (!this.enabled || !action || typeof action.undo !== 'function') return;
      this.redoStack.clear();
      this.undoStack.push(action);
      if (this.undoStack.length > this.maxEntries) {
        this.undoStack.items.shift();
      }
      console.log(`📚 [UndoManager] Acción registrada. Stack undo: ${this.undoStack.length}`);
      console.log(`📚 [UndoManager] Última acción:`, action.label || 'sin label');
    }

    async undo() {
      if (!this.enabled || this._busy) return false;
      const last = this.undoStack.pop();
      if (!last) {
        notificarUndo('No hay nada que deshacer', true);
        return false;
      }
      this._busy = true;
      console.log(`📚 [UndoManager] Deshaciendo:`, last.label || 'sin label');
      let ok = false;
      try {
        await last.undo();
        this.redoStack.push(last);
        notificarUndo(`Deshecho: ${last.label || 'última acción'}`);
        ok = true;
        return true;
      } catch (e) {
        console.error('[UndoManager] Error al deshacer:', e);
        this.undoStack.push(last);
        notificarUndo(e?.message || 'No se pudo deshacer el cambio', true);
        return false;
      } finally {
        this._busy = false;
        if (ok && !last.skipPageRefresh) {
          await this._refreshAfterChange();
        }
      }
    }

    async redo() {
      if (!this.enabled || this._busy) return false;
      const next = this.redoStack.pop();
      if (!next) {
        notificarUndo('No hay nada que rehacer', true);
        return false;
      }
      this._busy = true;
      let ok = false;
      try {
        if (typeof next.do === 'function') {
          await next.do();
        } else if (typeof next.redo === 'function') {
          await next.redo();
        } else {
          console.warn('[UndoManager] Acción no tiene do/redo; cancelado');
          this.redoStack.push(next);
          return false;
        }
        this.undoStack.push(next);
        notificarUndo(`Rehecho: ${next.label || 'última acción'}`);
        ok = true;
        return true;
      } catch (e) {
        console.error('[UndoManager] Error al rehacer:', e);
        this.redoStack.push(next);
        notificarUndo('No se pudo rehacer el cambio', true);
        return false;
      } finally {
        this._busy = false;
        if (ok && !next.skipPageRefresh) {
          await this._refreshAfterChange();
        }
      }
    }

    clear() {
      this.undoStack.clear();
      this.redoStack.clear();
    }

    async _refreshAfterChange() {
      if (typeof window.refrescarPaginaActual === 'function') {
        try {
          await window.refrescarPaginaActual();
        } catch (e) {
          console.warn('[UndoManager] Error refrescando vista:', e);
        }
      }
    }

    _installGlobalShortcuts() {
      document.addEventListener('focusin', (e) => {
        if (e.target && e.target.dataset && e.target.dataset.undoBaseline === undefined) {
          marcarBaseline(e.target);
        }
      });
      document.addEventListener('shown.bs.modal', (e) => {
        if (e && e.target) marcarBaselinesEn(e.target);
      });

      document.addEventListener('keydown', (e) => {
        const isCtrl = e.ctrlKey || e.metaKey;
        if (!isCtrl || e.altKey) return;

        const key = (e.key || '').toLowerCase();
        if (key !== 'z' && key !== 'y') return;

        const esRedo = key === 'y' || (key === 'z' && e.shiftKey);

        // Si el usuario está escribiendo y el texto ya no es el de al abrir,
        // el Ctrl+Z es el del navegador (deshacer letras).
        if (!esRedo && isTextEntryElement(e.target) && controlEstaSucio(e.target)) return;

        // En el menú de edición: si cambió el estado (u otro campo) y aún
        // no ha guardado, primero se restauran esos campos.
        if (!esRedo && restaurarFormularioModalAbierto()) {
          e.preventDefault();
          e.stopPropagation();
          notificarUndo('Deshecho: cambios del formulario');
          return;
        }

        e.preventDefault();
        e.stopPropagation();
        if (esRedo) {
          this.redo();
        } else {
          this.undo();
        }
      }, true);
    }
  }

  window.UndoManager = window.UndoManager || new UndoManagerImpl();

  window.handleGlobalUndo = async function () {
    try {
      if (window.UndoManager && typeof window.UndoManager.undo === 'function') {
        await window.UndoManager.undo();
      }
    } catch (e) {
      console.error('[UndoManager] Error en handleGlobalUndo:', e);
    }
  };

  window.handleGlobalRedo = async function () {
    try {
      if (window.UndoManager && typeof window.UndoManager.redo === 'function') {
        await window.UndoManager.redo();
      }
    } catch (e) {
      console.error('[UndoManager] Error en handleGlobalRedo:', e);
    }
  };
})();
