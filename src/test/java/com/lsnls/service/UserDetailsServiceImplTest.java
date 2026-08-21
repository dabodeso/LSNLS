package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    private UserDetailsServiceImpl service;

    @Mock
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl();
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepository);
    }

    @Test
    void loadUserByUsername_encontrado() {
        Usuario usuario = new Usuario();
        usuario.setNombre("admin");
        usuario.setPassword("hash");
        usuario.setRol(Usuario.RolUsuario.ROLE_ADMIN);
        when(usuarioRepository.findByNombre("admin")).thenReturn(Optional.of(usuario));

        UserDetails details = service.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("hash", details.getPassword());
        String authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());
        assertEquals("ROLE_ADMIN", authorities);
    }

    @Test
    void loadUserByUsername_noEncontrado() {
        when(usuarioRepository.findByNombre("ghost")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
        assertTrue(ex.getMessage().contains("ghost"));
    }
}
