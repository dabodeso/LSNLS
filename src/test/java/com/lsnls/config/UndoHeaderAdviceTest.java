package com.lsnls.config;

import com.lsnls.service.UndoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UndoHeaderAdviceTest {

    private UndoHeaderAdvice advice;

    @Mock
    private MethodParameter returnType;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @BeforeEach
    void setUp() {
        advice = new UndoHeaderAdvice();
        limpiarThreadLocal();
    }

    @AfterEach
    void tearDown() {
        limpiarThreadLocal();
    }

    @Test
    void supports_siempreTrue() {
        assertTrue(advice.supports(returnType, StringHttpMessageConverter.class));
    }

    @Test
    void beforeBodyWrite_sinOperacion_noAnadeCabecera() {
        Object body = advice.beforeBodyWrite("ok", returnType, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response);

        assertEquals("ok", body);
        assertNull(UndoService.consumirUltimaOperacionId());
    }

    @Test
    void beforeBodyWrite_conOperacion_anadeCabeceras() throws Exception {
        obtenerThreadLocal().set(77L);
        HttpHeaders headers = new HttpHeaders();
        when(response.getHeaders()).thenReturn(headers);

        Object body = advice.beforeBodyWrite("payload", returnType, MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, request, response);

        assertEquals("payload", body);
        assertEquals("77", headers.getFirst(UndoService.HEADER_OPERACION));
        assertEquals(UndoService.HEADER_OPERACION, headers.getFirst("Access-Control-Expose-Headers"));
        assertNull(UndoService.consumirUltimaOperacionId());
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<Long> obtenerThreadLocal() throws Exception {
        Field field = UndoService.class.getDeclaredField("ULTIMA_OPERACION");
        field.setAccessible(true);
        return (ThreadLocal<Long>) field.get(null);
    }

    private void limpiarThreadLocal() {
        try {
            obtenerThreadLocal().remove();
        } catch (Exception ignored) {
            UndoService.consumirUltimaOperacionId();
        }
    }
}
