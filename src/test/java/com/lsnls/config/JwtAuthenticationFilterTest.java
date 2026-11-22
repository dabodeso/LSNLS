package com.lsnls.config;

import com.lsnls.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rutaNoApi_continúaSinAutenticar() throws Exception {
        MockHttpServletRequest req = request("GET", "/login.html");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(200, res.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void rutaAuthPublica_sinToken() throws Exception {
        MockHttpServletRequest req = request("POST", "/api/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(200, res.getStatus());
    }

    @Test
    void apiSinToken_responde401() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(401, res.getStatus());
        assertEquals("application/json", res.getContentType());
    }

    @Test
    void htmlSinToken_permitePaso() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/pagina.html");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(200, res.getStatus());
    }

    @Test
    void tokenValido_estableceContexto() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        req.addHeader("Authorization", "Bearer tok123");
        UserDetails user = User.withUsername("admin").password("x").authorities("ROLE_ADMIN").build();
        when(jwtService.extractUsername("tok123")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        when(jwtService.isTokenValid("tok123", user)).thenReturn(true);

        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void tokenInvalido_responde401() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        req.addHeader("Authorization", "Bearer bad");
        UserDetails user = User.withUsername("admin").password("x").authorities("ROLE_ADMIN").build();
        when(jwtService.extractUsername("bad")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);
        when(jwtService.isTokenValid("bad", user)).thenReturn(false);

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(401, res.getStatus());
    }

    @Test
    void usernameNulo_responde401() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        req.addHeader("Authorization", "Bearer vacio");
        when(jwtService.extractUsername("vacio")).thenReturn(null);

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(401, res.getStatus());
    }

    @Test
    void yaAutenticado_continúa() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "x", Collections.emptyList()));
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        req.addHeader("Authorization", "Bearer tok");
        when(jwtService.extractUsername("tok")).thenReturn("admin");

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(200, res.getStatus());
    }

    @Test
    void excepcionAlLeerElToken_responde401() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        req.addHeader("Authorization", "Bearer tok");
        when(jwtService.extractUsername("tok")).thenThrow(new RuntimeException("boom"));

        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());

        assertEquals(401, res.getStatus());
        assertFalse(res.getContentAsString().contains("Error interno del servidor"));
    }

    @Test
    void excepcionDeLaPeticion_sigueSuCurso() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/preguntas");
        req.addHeader("Authorization", "Bearer tok");
        when(jwtService.extractUsername("tok")).thenReturn("admin");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "x", Collections.emptyList()));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws ServletException {
                ((MockHttpServletResponse) response).setStatus(403);
                throw new ServletException("fallo de negocio");
            }
        };

        ServletException error = assertThrows(ServletException.class, () -> filter.doFilter(req, res, chain));

        assertEquals("fallo de negocio", error.getMessage());
        assertEquals(403, res.getStatus());
        assertFalse(res.getContentAsString().contains("Error interno del servidor"));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setServletPath(path);
        return req;
    }
}
