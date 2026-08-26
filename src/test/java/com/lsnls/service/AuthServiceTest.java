package com.lsnls.service;

import com.lsnls.dto.AuthResponse;
import com.lsnls.dto.LoginRequest;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioAdmin() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("admin");
        usuario.setPassword("hash");
        usuario.setRol(Usuario.RolUsuario.ROLE_ADMIN);
        return usuario;
    }

    private UserDetails userDetails() {
        return User.builder().username("admin").password("hash").authorities("ROLE_ADMIN").build();
    }

    @Test
    void login_ok() {
        LoginRequest request = new LoginRequest();
        request.setNombre("admin");
        request.setPassword("pass");
        Usuario usuario = usuarioAdmin();
        UserDetails details = userDetails();

        when(usuarioRepository.findByNombre("admin")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("jwt");

        AuthResponse response = authService.login(request);

        assertEquals("jwt", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("admin", response.getNombre());
        assertEquals(Usuario.RolUsuario.ROLE_ADMIN, response.getRol());
    }

    @Test
    void login_rehashPasswordPlana() {
        LoginRequest request = new LoginRequest();
        request.setNombre("admin");
        request.setPassword("pass");
        Usuario usuario = usuarioAdmin();
        UserDetails details = userDetails();

        when(usuarioRepository.findByNombre("admin")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(passwordEncoder.upgradeEncoding("hash")).thenReturn(true);
        when(passwordEncoder.encode("pass")).thenReturn("bcrypt-hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("jwt");

        AuthResponse response = authService.login(request);

        assertEquals("jwt", response.getToken());
        verify(passwordEncoder).encode("pass");
        verify(usuarioRepository).save(usuario);
        assertEquals("bcrypt-hash", usuario.getPassword());
    }

    @Test
    void login_fail() {
        LoginRequest request = new LoginRequest();
        request.setNombre("admin");
        request.setPassword("mala");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Error en la autenticación"));
    }

    @Test
    void login_failUsuarioInexistente() {
        LoginRequest request = new LoginRequest();
        request.setNombre("ghost");
        request.setPassword("x");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().startsWith("Error en la autenticación"));
    }

    @Test
    void register_ok() {
        Usuario usuario = usuarioAdmin();
        usuario.setPassword("plain");
        UserDetails details = userDetails();

        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);
        when(jwtService.generateToken(details)).thenReturn("jwt2");

        AuthResponse response = authService.register(usuario);

        assertEquals("jwt2", response.getToken());
        assertEquals(5L, response.getId());
        verify(passwordEncoder).encode("plain");
    }

    @Test
    void register_duplicado() {
        Usuario usuario = usuarioAdmin();
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(usuario));
        assertEquals("El usuario ya existe", ex.getMessage());
    }

    @Test
    void register_errorGenerico() {
        Usuario usuario = usuarioAdmin();
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("db down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(usuario));
        assertTrue(ex.getMessage().contains("Error interno al registrar usuario"));
    }

    @Test
    void getCurrentUser_conAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin", "x", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(usuarioRepository.findByNombre("admin")).thenReturn(Optional.of(usuarioAdmin()));

        Optional<Usuario> result = authService.getCurrentUser();
        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getNombre());
        assertTrue(authService.isAuthenticated());
    }

    @Test
    void getCurrentUser_sinAuth() {
        SecurityContextHolder.clearContext();
        assertFalse(authService.getCurrentUser().isPresent());
        assertFalse(authService.isAuthenticated());
    }

    @Test
    void getCurrentUser_anonimo() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", "x", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(authService.getCurrentUser().isPresent());
    }

    @Test
    void getCurrentUser_noAutenticado() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin", "x");
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(authService.getCurrentUser().isPresent());
    }

    @Test
    void getCurrentUser_autenticadoPeroNoEnBd() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin", "x", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(usuarioRepository.findByNombre("admin")).thenReturn(Optional.empty());
        assertFalse(authService.getCurrentUser().isPresent());
    }

    @Test
    void logout_limpiaContexto() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin", "x", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        authService.logout();

        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                || SecurityContextHolder.getContext().getAuthentication() == null);
        // Tras clearContext la autenticación queda vacía
        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
    }

    @Test
    void validateToken_trueYFalse() {
        UserDetails details = userDetails();
        when(jwtService.extractUsername("ok")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);
        when(jwtService.isTokenValid("ok", details)).thenReturn(true);
        assertTrue(authService.validateToken("ok"));

        when(jwtService.extractUsername("bad")).thenThrow(new RuntimeException("invalid"));
        assertFalse(authService.validateToken("bad"));
    }
}
