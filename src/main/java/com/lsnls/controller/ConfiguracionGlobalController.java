package com.lsnls.controller;

import com.lsnls.entity.ConfiguracionGlobal;
import com.lsnls.service.ConfiguracionGlobalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ConfiguracionGlobalController {

    @Autowired
    private ConfiguracionGlobalService configuracionService;

    @GetMapping
    public ResponseEntity<List<ConfiguracionGlobal>> findAll() {
        return ResponseEntity.ok(configuracionService.findAll());
    }

    @PutMapping("/{clave}")
    public ResponseEntity<?> actualizar(
            @PathVariable String clave, 
            @RequestBody Map<String, Object> data) {
        try {
            String valor = (String) data.get("valor");
            String descripcion = (String) data.get("descripcion");
            
            ConfiguracionGlobal actualizada = configuracionService.actualizarConfiguracion(clave, valor, descripcion);
            return ResponseEntity.ok(actualizada);
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("La configuración ha sido modificada por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar configuración: " + e.getMessage());
        }
    }

    @GetMapping("/duracion-objetivo")
    public ResponseEntity<String> getDuracionObjetivo() {
        return ResponseEntity.ok(configuracionService.getDuracionObjetivo());
    }

    @PutMapping("/duracion-objetivo")
    public ResponseEntity<?> setDuracionObjetivo(@RequestBody Map<String, String> data) {
        try {
            String duracion = data.get("duracion");
            configuracionService.setDuracionObjetivo(duracion);
            return ResponseEntity.ok("Duración objetivo actualizada correctamente");
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body("La configuración de duración ha sido modificada por otro usuario. Por favor, recarga e intenta nuevamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar duración objetivo: " + e.getMessage());
        }
    }
} 