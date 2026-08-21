package com.lsnls.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails otroUsuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    private UserDetails usuario(String nombre) {
        return User.builder()
                .username(nombre)
                .password("secret")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void generateTokenYExtractUsername() {
        UserDetails user = usuario("admin");
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
    }

    @Test
    void generateToken_conClaimsExtra() {
        UserDetails user = usuario("guion");
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("rol", "ROLE_GUION");

        String token = jwtService.generateToken(claims, user);
        assertEquals("guion", jwtService.extractUsername(token));
        assertEquals("ROLE_GUION", jwtService.extractClaim(token, c -> c.get("rol", String.class)));
    }

    @Test
    void isTokenValid_mismoUsuario_true() {
        UserDetails user = usuario("admin");
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_usuarioDistinto_false() {
        UserDetails user = usuario("admin");
        String token = jwtService.generateToken(user);

        org.mockito.Mockito.when(otroUsuario.getUsername()).thenReturn("otro");
        assertFalse(jwtService.isTokenValid(token, otroUsuario));
    }
}
