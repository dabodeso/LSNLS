package com.lsnls.config;

import com.lsnls.dto.ApiResponse;
import com.lsnls.dto.ErrorResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensajesUsuarioTest {

    @Test
    void sanitizar_conservaMensajeDeNegocio() {
        assertEquals("El nombre es obligatorio", MensajesUsuario.sanitizar("El nombre es obligatorio"));
    }

    @Test
    void sanitizar_ocultaSqlYHibernate() {
        String texto = MensajesUsuario.sanitizar(
                "Error interno: could not execute statement; nested exception is java.sql.SQLException: Duplicate entry");
        assertEquals(MensajesUsuario.RELACIONADOS, texto);
        assertFalse(texto.toLowerCase().contains("sql"));
        assertFalse(texto.contains("java."));
    }

    @Test
    void sanitizar_ocultaFailedToFetch() {
        assertEquals(MensajesUsuario.RED, MensajesUsuario.sanitizar("Failed to fetch"));
    }

    @Test
    void sanitizar_ocultaTokenJwt() {
        assertEquals(MensajesUsuario.SESION, MensajesUsuario.sanitizar("UNAUTHORIZED: Token expirado o inválido"));
    }

    @Test
    void sanitizar_ocultaOptimisticLock() {
        assertEquals(MensajesUsuario.CONCURRENCIA,
                MensajesUsuario.sanitizar("ObjectOptimisticLockingFailureException: Row was updated or deleted"));
    }

    @Test
    void sanitizar_quitaEmojis() {
        assertEquals("No se pudo guardar", MensajesUsuario.sanitizar("No se pudo guardar"));
        assertEquals("Error al guardar", MensajesUsuario.quitarEmojis("Error al guardar"));
    }

    @Test
    void de_usaAccionSiElTextoEsTecnico() {
        String texto = MensajesUsuario.de(new RuntimeException("org.hibernate.QueryException"), "guardar el combo");
        assertEquals("No se ha podido guardar el combo. Inténtalo de nuevo.", texto);
    }

    @Test
    void porHttp_cubreEstadosHabituales() {
        assertEquals(MensajesUsuario.SESION, MensajesUsuario.porHttp(401));
        assertEquals(MensajesUsuario.PERMISOS, MensajesUsuario.porHttp(403));
        assertEquals(MensajesUsuario.NO_ENCONTRADO, MensajesUsuario.porHttp(404));
        assertEquals(MensajesUsuario.VALIDACION, MensajesUsuario.porHttp(400));
        assertEquals(MensajesUsuario.GENERICO, MensajesUsuario.porHttp(500));
    }

    @Test
    void advice_sanitizaStringYErrorResponse() {
        String sanitizado = (String) ErrorResponseAdvice.sanitizarCuerpo(
                "java.sql.SQLException: timeout", 500);
        assertEquals(MensajesUsuario.GENERICO, sanitizado);

        ErrorResponse error = new ErrorResponse("org.hibernate.exception.GenericJDBCException",
                "could not execute statement");
        ErrorResponse resultado = (ErrorResponse) ErrorResponseAdvice.sanitizarCuerpo(error, 500);
        assertEquals("Error", resultado.getError());
        assertEquals(MensajesUsuario.GENERICO, resultado.getMensaje());
    }

    @Test
    void advice_conservaApiResponseDeNegocio() {
        ApiResponse<Void> api = ApiResponse.error("El cuestionario está asignado a una jornada");
        @SuppressWarnings("unchecked")
        ApiResponse<Void> resultado = (ApiResponse<Void>) ErrorResponseAdvice.sanitizarCuerpo(api, 400);
        assertEquals("El cuestionario está asignado a una jornada", resultado.getMensaje());
        assertFalse(resultado.isExito());
    }

    @Test
    void advice_sanitizaMapaSpring() {
        @SuppressWarnings("unchecked")
        Map<String, String> resultado = (Map<String, String>) ErrorResponseAdvice.sanitizarCuerpo(
                Map.of("message", "could not extract ResultSet", "error", "Internal Server Error"),
                500);
        assertEquals(MensajesUsuario.GENERICO, resultado.get("message"));
        assertTrue(resultado.get("error").equals("Error") || resultado.get("error").equals(MensajesUsuario.GENERICO));
    }
}
