package com.lsnls.controller;

import com.lsnls.entity.ConfiguracionGlobal;
import com.lsnls.service.ConfiguracionGlobalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionGlobalControllerTest {

    @Mock
    private ConfiguracionGlobalService configuracionService;

    @InjectMocks
    private ConfiguracionGlobalController configuracionGlobalController;

    @Test
    void findAll_ok_devuelve200() {
        ConfiguracionGlobal cfg = new ConfiguracionGlobal("k", "v", "d");
        when(configuracionService.findAll()).thenReturn(Collections.singletonList(cfg));

        ResponseEntity<List<ConfiguracionGlobal>> response = configuracionGlobalController.findAll();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void actualizar_ok_devuelve200() {
        ConfiguracionGlobal actualizada = new ConfiguracionGlobal("clave", "nuevo", "desc");
        when(configuracionService.actualizarConfiguracion("clave", "nuevo", "desc")).thenReturn(actualizada);
        Map<String, Object> data = new HashMap<>();
        data.put("valor", "nuevo");
        data.put("descripcion", "desc");

        ResponseEntity<?> response = configuracionGlobalController.actualizar("clave", data);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(actualizada, response.getBody());
    }

    @Test
    void actualizar_optimisticLock_devuelve409() {
        when(configuracionService.actualizarConfiguracion("clave", "v", null))
                .thenThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()));
        Map<String, Object> data = new HashMap<>();
        data.put("valor", "v");

        ResponseEntity<?> response = configuracionGlobalController.actualizar("clave", data);

        assertEquals(409, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("modificada por otro usuario"));
    }

    @Test
    void actualizar_excepcion_devuelve400() {
        when(configuracionService.actualizarConfiguracion("clave", "v", null))
                .thenThrow(new RuntimeException("no existe"));
        Map<String, Object> data = new HashMap<>();
        data.put("valor", "v");

        ResponseEntity<?> response = configuracionGlobalController.actualizar("clave", data);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("no existe"));
    }

    @Test
    void getDuracionObjetivo_ok_devuelve200() {
        when(configuracionService.getDuracionObjetivo()).thenReturn("45:00");

        ResponseEntity<String> response = configuracionGlobalController.getDuracionObjetivo();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("45:00", response.getBody());
    }

    @Test
    void setDuracionObjetivo_ok_devuelve200() {
        doNothing().when(configuracionService).setDuracionObjetivo("50:00");
        Map<String, String> data = new HashMap<>();
        data.put("duracion", "50:00");

        ResponseEntity<?> response = configuracionGlobalController.setDuracionObjetivo(data);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Duración objetivo actualizada correctamente", response.getBody());
    }

    @Test
    void setDuracionObjetivo_optimisticLock_devuelve409() {
        doThrow(new ObjectOptimisticLockingFailureException("conflicto", new RuntimeException()))
                .when(configuracionService).setDuracionObjetivo("50:00");
        Map<String, String> data = new HashMap<>();
        data.put("duracion", "50:00");

        ResponseEntity<?> response = configuracionGlobalController.setDuracionObjetivo(data);

        assertEquals(409, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("modificada por otro usuario"));
    }

    @Test
    void setDuracionObjetivo_excepcion_devuelve400() {
        doThrow(new RuntimeException("formato")).when(configuracionService).setDuracionObjetivo("xx");
        Map<String, String> data = new HashMap<>();
        data.put("duracion", "xx");

        ResponseEntity<?> response = configuracionGlobalController.setDuracionObjetivo(data);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains("formato"));
    }
}
