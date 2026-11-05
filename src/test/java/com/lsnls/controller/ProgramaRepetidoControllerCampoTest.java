package com.lsnls.controller;

import com.lsnls.dto.ConcursanteEmitidoOpcionDTO;
import com.lsnls.dto.ProgramaEmitidoOpcionDTO;
import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.dto.ResultadoBusquedaDTO;
import com.lsnls.service.ProgramaRepetidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramaRepetidoControllerCampoTest {

    @Mock
    private ProgramaRepetidoService programaRepetidoService;

    @InjectMocks
    private ProgramaRepetidoController controller;

    @Test
    void updateCampo_ok() {
        ProgramaRepetidoDTO dto = new ProgramaRepetidoDTO();
        dto.setId(7L);
        dto.setEstado("grabado");
        Map<String, Object> campo = Map.of("estado", "grabado");
        when(programaRepetidoService.updateCampo(7L, campo)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateCampo(7L, campo);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("grabado", ((ProgramaRepetidoDTO) response.getBody()).getEstado());
    }

    @Test
    void updateCampo_vacio_devuelve400() {
        ResponseEntity<?> response = controller.updateCampo(7L, new HashMap<>());

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void updateCampo_emitido_devuelve400() {
        when(programaRepetidoService.updateCampo(7L, Map.of("fechaEmision", "2026-11-05")))
                .thenThrow(new IllegalArgumentException("Un programa emitido no se puede editar."));

        ResponseEntity<?> response = controller.updateCampo(7L, Map.of("fechaEmision", "2026-11-05"));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(String.valueOf(response.getBody()).contains("emitido"));
    }

    @Test
    void findById_ok() {
        ProgramaRepetidoDTO dto = new ProgramaRepetidoDTO();
        dto.setId(7L);
        when(programaRepetidoService.findById(7L)).thenReturn(dto);

        ResponseEntity<?> response = controller.findById(7L);

        assertEquals(200, response.getStatusCodeValue());
        verify(programaRepetidoService).findById(7L);
    }

    @Test
    void buscarProgramasEmitidos_devuelveLaPaginaPedida() {
        ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> dto = new ResultadoBusquedaDTO<>();
        dto.setTotal(40L);
        dto.setPage(1);
        dto.setSize(ProgramaRepetidoService.TAMANO_PAGINA_PROGRAMAS_ORIGEN);
        dto.setHayMas(false);
        dto.setResultados(List.of());
        when(programaRepetidoService.buscarProgramasEmitidos("2026-01-01", "12", "2", 1)).thenReturn(dto);

        ResponseEntity<?> response = controller.buscarProgramasEmitidos("2026-01-01", "12", "2", 1);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        ResultadoBusquedaDTO<?> body = (ResultadoBusquedaDTO<?>) response.getBody();
        assertEquals(40L, body.getTotal());
        assertEquals(1, body.getPage());
        assertEquals(25, body.getSize());
        verify(programaRepetidoService).buscarProgramasEmitidos("2026-01-01", "12", "2", 1);
    }

    @Test
    void buscarConcursantesEmitidos_devuelveLaPaginaPedida() {
        ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> dto = new ResultadoBusquedaDTO<>();
        dto.setTotal(130L);
        dto.setPage(1);
        dto.setSize(ProgramaRepetidoService.TAMANO_PAGINA_CONCURSANTES_ORIGEN);
        dto.setHayMas(true);
        dto.setResultados(List.of());
        when(programaRepetidoService.buscarConcursantesEmitidos("2", "01:30", "4", "Bilbao", "emitido", 1, 25))
                .thenReturn(dto);

        ResponseEntity<?> response = controller.buscarConcursantesEmitidos(
                "2", "01:30", "4", "Bilbao", "emitido", 1, 25);

        assertEquals(200, response.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        ResultadoBusquedaDTO<?> body = (ResultadoBusquedaDTO<?>) response.getBody();
        assertEquals(130L, body.getTotal());
        assertEquals(1, body.getPage());
        assertEquals(25, body.getSize());
        assertTrue(body.isHayMas());
        verify(programaRepetidoService).buscarConcursantesEmitidos("2", "01:30", "4", "Bilbao", "emitido", 1, 25);
    }

    @Test
    void findById_noEncontrado_devuelve404() {
        when(programaRepetidoService.findById(9L))
                .thenThrow(new IllegalArgumentException("Programa repetido no encontrado"));

        ResponseEntity<?> response = controller.findById(9L);

        assertEquals(404, response.getStatusCodeValue());
    }
}
