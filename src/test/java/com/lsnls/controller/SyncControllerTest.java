package com.lsnls.controller;

import com.lsnls.dto.EntityChangeDTO;
import com.lsnls.dto.VisibleEntityDTO;
import com.lsnls.service.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private SyncService syncService;

    @InjectMocks
    private SyncController syncController;

    @Test
    void visibleChanges_ok_devuelveCambios() {
        VisibleEntityDTO item = new VisibleEntityDTO();
        item.setEntityType("PREGUNTA");
        item.setEntityId(1L);
        item.setVersion(2L);
        List<VisibleEntityDTO> items = Collections.singletonList(item);
        List<EntityChangeDTO> changes = Collections.singletonList(
                new EntityChangeDTO("PREGUNTA", 1L, "pregunta", "admin", 3L, "cambio"));
        when(syncService.checkVisibleChanges(items)).thenReturn(changes);

        ResponseEntity<Map<String, Object>> response = syncController.visibleChanges(items);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(changes, response.getBody().get("changes"));
    }

    @Test
    void visibleChanges_listaVacia_devuelveOk() {
        List<VisibleEntityDTO> items = Collections.emptyList();
        when(syncService.checkVisibleChanges(items)).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = syncController.visibleChanges(items);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(Collections.emptyList(), response.getBody().get("changes"));
    }
}
