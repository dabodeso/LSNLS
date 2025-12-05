// Gestor global de deshacer/rehacer con atajos Ctrl+Z / Ctrl+Y
// API pública:
//   UndoManager.record({ do: fnDo, undo: fnUndo, label?: string })
//   UndoManager.undo()
//   UndoManager.redo()
//   UndoManager.clear()
//   UndoManager.enabled = true/false
// Notas:
// - Sólo registra acciones tras éxito (llamar record después de actualizar UI y backend)
// - Ignora atajos en inputs/textarea/select y contentEditable

(function () {
  const isEditableElement = (el) => {
    if (!el) return false;
    const tag = el.tagName ? el.tagName.toLowerCase() : '';
    if (tag === 'input' || tag === 'textarea' || tag === 'select') return true;
    if (el.isContentEditable) return true;
    return false;
  };

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
      this.maxEntries = 500; // límite razonable
      this._installGlobalShortcuts();
    }

    record(action) {
      if (!this.enabled || !action || typeof action.undo !== 'function') return;
      // Al registrar nueva acción, se invalida el redo
      this.redoStack.clear();
      this.undoStack.push(action);
      // Recortar si excede límite
      if (this.undoStack.length > this.maxEntries) {
        this.undoStack.items.shift();
      }
      console.log(`📚 [UndoManager] Acción registrada. Stack undo: ${this.undoStack.length}, Stack redo: ${this.redoStack.length}`);
      console.log(`📚 [UndoManager] Última acción:`, action.label || 'sin label');
    }

    async undo() {
      if (!this.enabled) return;
      console.log(`📚 [UndoManager] UNDO solicitado. Stack undo antes: ${this.undoStack.length}, Stack redo antes: ${this.redoStack.length}`);
      const last = this.undoStack.pop();
      if (!last) {
        console.log('📚 [UndoManager] No hay acciones para deshacer');
        return;
      }
      console.log(`📚 [UndoManager] Deshaciendo:`, last.label || 'sin label');
      try {
        await last.undo();
        // Permitir rehacer con la operación inversa original (si existe .do)
        this.redoStack.push(last);
        console.log(`📚 [UndoManager] UNDO exitoso. Stack undo después: ${this.undoStack.length}, Stack redo después: ${this.redoStack.length}`);
      } catch (e) {
        console.error('[UndoManager] Error al deshacer:', e);
        // Si falla el undo, NO volver a meter la acción al stack (se perdió)
        // Esto evita que se quede atascado intentando deshacer algo que falla
        console.warn('[UndoManager] Acción descartada por error. No se puede deshacer.');
      }
    }

    async redo() {
      if (!this.enabled) return;
      const next = this.redoStack.pop();
      if (!next) return;
      try {
        if (typeof next.do === 'function') {
          await next.do();
        } else if (typeof next.redo === 'function') {
          await next.redo();
        } else {
          // Si no hay do/redo, re-aplicar llamando al inverso otra vez es inseguro
          console.warn('[UndoManager] Acción no tiene do/redo; cancelado');
          return;
        }
        this.undoStack.push(next);
      } catch (e) {
        console.error('[UndoManager] Error al rehacer:', e);
      }
    }

    clear() {
      this.undoStack.clear();
      this.redoStack.clear();
    }

    _installGlobalShortcuts() {
      document.addEventListener('keydown', (e) => {
        // Evitar interferir cuando usuario escribe en un campo editable
        if (isEditableElement(e.target)) return;

        const isCtrl = e.ctrlKey || e.metaKey; // Soporte Cmd en Mac
        if (!isCtrl) return;

        // Ctrl+Z => undo, Ctrl+Y => redo, Ctrl+Shift+Z => redo (estándar Mac)
        const key = (e.key || '').toLowerCase();
        if (key === 'z') {
          e.preventDefault();
          if (e.shiftKey) {
            this.redo();
          } else {
            this.undo();
          }
        } else if (key === 'y') {
          e.preventDefault();
          this.redo();
        }
      });
    }
  }

  // Exponer singleton global
  window.UndoManager = window.UndoManager || new UndoManagerImpl();
})();


