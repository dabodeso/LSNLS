package com.lsnls.security;

import com.lsnls.config.JwtAuthenticationFilter;
import com.lsnls.config.SecurityConfig;
import com.lsnls.controller.AuthController;
import com.lsnls.dto.AuthResponse;
import com.lsnls.entity.Usuario;
import com.lsnls.service.AuthService;
import com.lsnls.service.JwtService;
import com.lsnls.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Arranca el filtro JWT y SecurityConfig de verdad (sin MySQL).
 * Login es público; /api/auth/me exige token.
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ApiAuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void meSinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("sesión")));
    }

    @Test
    void loginSinToken_llegaAlControlador() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("ana");
        usuario.setRol(Usuario.RolUsuario.ROLE_GUION);
        AuthResponse respuesta = new AuthResponse("jwt-de-prueba", usuario);
        when(authService.login(any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"ana\",\"password\":\"capote\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-de-prueba"))
            .andExpect(jsonPath("$.nombre").value("ana"));
    }

    @Test
    void loginPasswordVacia_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"ana\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerSinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"nuevo\",\"password\":\"capote\",\"rol\":\"ROLE_GUION\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(containsString("sesión")));
    }
}
