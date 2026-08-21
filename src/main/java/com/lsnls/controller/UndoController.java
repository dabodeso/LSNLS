package com.lsnls.controller;

import com.lsnls.dto.ApiResponse;
import com.lsnls.service.UndoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/undo")
@CrossOrigin(origins = "*")
public class UndoController {

    @Autowired
    private UndoService undoService;

    /**
     * Revierte una operación deshacible. Solo el autor de la operación puede
     * deshacerla, una única vez y dentro de la hora siguiente a realizarla.
     */
    @PostMapping("/{operacionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deshacer(@PathVariable Long operacionId) {
        try {
            String descripcion = undoService.deshacer(operacionId);
            String mensaje = descripcion != null && !descripcion.isEmpty()
                    ? "Deshecho: " + descripcion
                    : "Operación deshecha";
            return ResponseEntity.ok(ApiResponse.exitoso(mensaje, descripcion));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al deshacer la operación: " + e.getMessage()));
        }
    }
}
