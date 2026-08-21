package com.lsnls.config;

import com.lsnls.service.UndoService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Añade a la respuesta HTTP la cabecera X-Undo-Operacion-Id cuando la petición
 * registró una operación deshacible, para que el frontend pueda ofrecer Ctrl+Z
 * llamando a POST /api/undo/{id}.
 */
@RestControllerAdvice
public class UndoHeaderAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        Long operacionId = UndoService.consumirUltimaOperacionId();
        if (operacionId != null) {
            response.getHeaders().set(UndoService.HEADER_OPERACION, String.valueOf(operacionId));
            response.getHeaders().set("Access-Control-Expose-Headers", UndoService.HEADER_OPERACION);
        }
        return body;
    }
}
