package com.lsnls.config;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.ErrorResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Última barrera: si un controlador devuelve un 4xx/5xx con texto técnico
 * (SQL, Hibernate, getMessage de Java), se sustituye antes de enviarlo al navegador.
 */
@ControllerAdvice
public class ErrorResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        int status = extraerStatus(response);
        if (status < 400) {
            return body;
        }
        return sanitizarCuerpo(body, status);
    }

    private static int extraerStatus(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse) {
            HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
            if (servletResponse != null) {
                return servletResponse.getStatus();
            }
        }
        return 500;
    }

    static Object sanitizarCuerpo(Object body, int status) {
        String fallback = MensajesUsuario.porHttp(status);
        if (body == null) {
            return Map.of("mensaje", fallback, "message", fallback);
        }
        if (body instanceof String) {
            return MensajesUsuario.sanitizar((String) body, fallback);
        }
        if (body instanceof ApiResponse) {
            @SuppressWarnings("unchecked")
            ApiResponse<Object> api = (ApiResponse<Object>) body;
            if (!api.isExito()) {
                api.setMensaje(MensajesUsuario.sanitizar(api.getMensaje(), fallback));
            }
            return api;
        }
        if (body instanceof ErrorResponse) {
            ErrorResponse error = (ErrorResponse) body;
            error.setMensaje(MensajesUsuario.sanitizar(error.getMensaje(), fallback));
            if (MensajesUsuario.esTecnico(error.getError())) {
                error.setError("Error");
            }
            return error;
        }
        if (body instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> original = (Map<Object, Object>) body;
            Map<Object, Object> copia = new LinkedHashMap<>(original);
            for (String clave : new String[] {"mensaje", "message", "detail", "error", "title"}) {
                Object valor = copia.get(clave);
                if (valor instanceof String) {
                    String sanitizado = MensajesUsuario.sanitizar((String) valor, fallback);
                    if ("error".equals(clave) && MensajesUsuario.esTecnico((String) valor)
                            && sanitizado.equals(fallback)) {
                        copia.put(clave, "Error");
                    } else {
                        copia.put(clave, sanitizado);
                    }
                }
            }
            return copia;
        }
        return body;
    }
}
