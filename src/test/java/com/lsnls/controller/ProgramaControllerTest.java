package com.lsnls.controller;

import com.lsnls.dto.ProgramaDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.service.EditLockService;
import com.lsnls.service.ProgramaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramaControllerTest {

    @Mock
    private ProgramaService programaService;

    @Mock
    private EditLockService editLockService;

    @InjectMocks
    private ProgramaController programaController;

    private ProgramaDTO programa(Long id, Integer temporada) {
        ProgramaDTO dto = new ProgramaDTO();
        dto.setId(id);
        dto.setTemporada(temporada);
        return dto;
    }

    @Test
    void findAll_ok_devuelve200() {
        when(programaService.findAllDTO()).thenReturn(Collections.singletonList(programa(1L, 1)));

        ResponseEntity<List<ProgramaDTO>> response = programaController.findAll();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void findAllPaginated_okDesc_devuelve200() {
        Map<String, Object> page = Collections.singletonMap("total", 1);
        when(programaService.findAllPaginated(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Map<String, Object>> response = programaController.findAllPaginated(0, 10, "id", "desc");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(page, response.getBody());
    }

    @Test
    void findAllPaginated_okAsc_devuelve200() {
        when(programaService.findAllPaginated(any(Pageable.class))).thenReturn(Collections.emptyMap());

        ResponseEntity<Map<String, Object>> response = programaController.findAllPaginated(0, 10, "id", "asc");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void findById_ok_devuelve200() {
        ProgramaDTO dto = programa(1L, 1);
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<?> response = programaController.findById(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    void findById_noEncontrado_devuelve404() {
        when(programaService.findByIdDTO(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = programaController.findById(9L);

        assertEquals(404, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no encontrado"));
    }

    @Test
    void findById_excepcion_devuelve500() {
        when(programaService.findByIdDTO(1L)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = programaController.findById(1L);

        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void create_temporadaNula_devuelve400() {
        ResponseEntity<?> response = programaController.create(programa(null, null));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("temporada"));
    }

    @Test
    void create_ok_devuelve200() {
        ProgramaDTO dto = programa(null, 2);
        ProgramaDTO creado = programa(5L, 2);
        when(programaService.createFromDTO(dto)).thenReturn(creado);

        ResponseEntity<?> response = programaController.create(dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(creado, response.getBody());
    }

    @Test
    void create_optimisticLock_devuelve409() {
        ProgramaDTO dto = programa(null, 2);
        when(programaService.createFromDTO(dto))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = programaController.create(dto);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void create_illegalArgument_devuelve400() {
        ProgramaDTO dto = programa(null, 2);
        when(programaService.createFromDTO(dto)).thenThrow(new IllegalArgumentException("duplicado"));

        ResponseEntity<?> response = programaController.create(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de validación"));
    }

    @Test
    void create_excepcion_devuelve400() {
        ProgramaDTO dto = programa(null, 2);
        when(programaService.createFromDTO(dto)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = programaController.create(dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error interno"));
    }

    @Test
    void update_noEncontrado_devuelve404() {
        when(programaService.findByIdDTO(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = programaController.update(9L, programa(9L, 1));

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void update_temporadaNula_devuelve400() {
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));

        ResponseEntity<?> response = programaController.update(1L, programa(1L, null));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("temporada"));
    }

    @Test
    void update_ok_devuelve200() {
        ProgramaDTO dto = programa(1L, 3);
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(dto));
        doNothing().when(editLockService).assertCanEdit(AuditLog.EntityType.PROGRAMA, 1L);
        when(programaService.updateFromDTO(1L, dto)).thenReturn(dto);
        doNothing().when(editLockService).logEntityUpdate(AuditLog.EntityType.PROGRAMA, 1L, "Actualización de programa");

        ResponseEntity<?> response = programaController.update(1L, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
        verify(editLockService).assertCanEdit(AuditLog.EntityType.PROGRAMA, 1L);
    }

    @Test
    void update_optimisticLock_devuelve409() {
        ProgramaDTO dto = programa(1L, 3);
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(dto));
        when(programaService.updateFromDTO(1L, dto))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = programaController.update(1L, dto);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void update_illegalArgument_devuelve400() {
        ProgramaDTO dto = programa(1L, 3);
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(dto));
        when(programaService.updateFromDTO(1L, dto)).thenThrow(new IllegalArgumentException("inválido"));

        ResponseEntity<?> response = programaController.update(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de validación"));
    }

    @Test
    void update_excepcion_devuelve400() {
        ProgramaDTO dto = programa(1L, 3);
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(dto));
        when(programaService.updateFromDTO(1L, dto)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = programaController.update(1L, dto);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void updateDuracionObjetivo_vacia_devuelve400() {
        Map<String, String> request = new HashMap<>();
        request.put("duracionObjetivo", "  ");

        ResponseEntity<?> response = programaController.updateDuracionObjetivo(1L, request);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("obligatoria"));
    }

    @Test
    void updateDuracionObjetivo_ok_devuelve200() {
        doNothing().when(programaService).updateDuracionObjetivo(1L, "45:00");
        Map<String, String> request = new HashMap<>();
        request.put("duracionObjetivo", "45:00");

        ResponseEntity<?> response = programaController.updateDuracionObjetivo(1L, request);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void updateDuracionObjetivo_excepcion_devuelve400() {
        doThrow(new RuntimeException("fail")).when(programaService).updateDuracionObjetivo(1L, "45:00");
        Map<String, String> request = new HashMap<>();
        request.put("duracionObjetivo", "45:00");

        ResponseEntity<?> response = programaController.updateDuracionObjetivo(1L, request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void updateCampo_noEncontrado_devuelve404() {
        when(programaService.findByIdDTO(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = programaController.updateCampo(9L, Collections.singletonMap("notas", "x"));

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void updateCampo_vacio_devuelve400() {
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));

        ResponseEntity<?> response = programaController.updateCampo(1L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("al menos un campo"));
    }

    @Test
    void updateCampo_ok_devuelve200() {
        Map<String, Object> campo = Collections.singletonMap("notas", "ok");
        ProgramaDTO updated = programa(1L, 1);
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(updated));
        when(programaService.updateCampo(1L, campo)).thenReturn(updated);

        ResponseEntity<?> response = programaController.updateCampo(1L, campo);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(updated, response.getBody());
    }

    @Test
    void updateCampo_optimisticLock_devuelve409() {
        Map<String, Object> campo = Collections.singletonMap("notas", "ok");
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        when(programaService.updateCampo(1L, campo))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));

        ResponseEntity<?> response = programaController.updateCampo(1L, campo);

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void updateCampo_illegalArgument_devuelve400() {
        Map<String, Object> campo = Collections.singletonMap("notas", "ok");
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        when(programaService.updateCampo(1L, campo)).thenThrow(new IllegalArgumentException("campo inválido"));

        ResponseEntity<?> response = programaController.updateCampo(1L, campo);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("Error de validación"));
    }

    @Test
    void updateCampo_excepcion_devuelve400() {
        Map<String, Object> campo = Collections.singletonMap("notas", "ok");
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        when(programaService.updateCampo(1L, campo)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<?> response = programaController.updateCampo(1L, campo);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void delete_noEncontrado_devuelve404() {
        when(programaService.findByIdDTO(9L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = programaController.delete(9L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void delete_ok_devuelve200() {
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        doNothing().when(programaService).delete(1L);

        ResponseEntity<?> response = programaController.delete(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Programa eliminado exitosamente", response.getBody());
    }

    @Test
    void delete_illegalArgument_devuelve400() {
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        doThrow(new IllegalArgumentException("no se puede")).when(programaService).delete(1L);

        ResponseEntity<?> response = programaController.delete(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("no se puede", response.getBody());
    }

    @Test
    void delete_dataIntegrity_devuelve400() {
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        doThrow(new DataIntegrityViolationException("fk")).when(programaService).delete(1L);

        ResponseEntity<?> response = programaController.delete(1L);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("concursantes asociados"));
    }

    @Test
    void delete_excepcion_devuelve400() {
        when(programaService.findByIdDTO(1L)).thenReturn(Optional.of(programa(1L, 1)));
        doThrow(new RuntimeException("fail")).when(programaService).delete(1L);

        ResponseEntity<?> response = programaController.delete(1L);

        assertEquals(400, response.getStatusCodeValue());
    }
}
