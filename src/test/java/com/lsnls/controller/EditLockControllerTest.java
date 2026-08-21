package com.lsnls.controller;

import com.lsnls.dto.EditLockRequestDTO;
import com.lsnls.entity.AuditLog;
import com.lsnls.service.EditLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditLockControllerTest {

    @Mock
    private EditLockService editLockService;

    @InjectMocks
    private EditLockController editLockController;

    private EditLockRequestDTO request(String type, Long id) {
        EditLockRequestDTO dto = new EditLockRequestDTO();
        dto.setEntityType(type);
        dto.setEntityId(id);
        return dto;
    }

    @Test
    void acquire_ok_devuelve200() {
        Map<String, Object> result = Collections.singletonMap("acquired", true);
        when(editLockService.acquire(AuditLog.EntityType.PREGUNTA, 5L)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = editLockController.acquire(request("PREGUNTA", 5L));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(result, response.getBody());
    }

    @Test
    void acquire_tipoInvalido_lanzaBadRequest() {
        assertThrows(ResponseStatusException.class,
                () -> editLockController.acquire(request("NO_EXISTE", 1L)));
    }

    @Test
    void renew_ok_devuelve200() {
        Map<String, Object> result = Collections.singletonMap("renewed", true);
        when(editLockService.renew(AuditLog.EntityType.COMBO, 3L)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = editLockController.renew(request("combo", 3L));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(result, response.getBody());
    }

    @Test
    void release_ok_devuelve204() {
        doNothing().when(editLockService).release(AuditLog.EntityType.CUESTIONARIO, 9L);

        ResponseEntity<Void> response = editLockController.release(request("CUESTIONARIO", 9L));

        assertEquals(204, response.getStatusCodeValue());
        verify(editLockService).release(AuditLog.EntityType.CUESTIONARIO, 9L);
    }

    @Test
    void status_ok_devuelve200() {
        Map<String, Object> result = Collections.singletonMap("locked", false);
        when(editLockService.status(AuditLog.EntityType.JORNADA, 2L)).thenReturn(result);

        ResponseEntity<Map<String, Object>> response = editLockController.status("JORNADA", 2L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(result, response.getBody());
    }

    @Test
    void status_tipoInvalido_lanzaBadRequest() {
        assertThrows(ResponseStatusException.class,
                () -> editLockController.status("XYZ", 1L));
    }
}
