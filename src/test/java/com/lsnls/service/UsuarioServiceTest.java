package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("admin");
        usuario.setPassword("old");
        usuario.setRol(Usuario.RolUsuario.ROLE_ADMIN);
        usuario.setVersion(3L);
    }

    @SuppressWarnings("unchecked")
    private void mockCounts(Long... values) {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        if (values.length == 1) {
            when(query.getSingleResult()).thenReturn(values[0]);
        } else {
            when(query.getSingleResult()).thenReturn(values[0], java.util.Arrays.copyOfRange(values, 1, values.length));
        }
    }

    @Test
    void crear_asignaPasswordPorDefecto() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("123456")).thenReturn("hash-123456");
        Usuario creado = usuarioService.crear(new Usuario());
        assertEquals("hash-123456", creado.getPassword());
    }

    @Test
    void obtenerTodosPorIdYNombre() {
        when(usuarioRepository.findAll()).thenReturn(Collections.singletonList(usuario));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByNombre("admin")).thenReturn(Optional.of(usuario));

        List<Usuario> todos = usuarioService.obtenerTodos();
        assertEquals(1, todos.size());
        assertTrue(usuarioService.obtenerPorId(1L).isPresent());
        assertTrue(usuarioService.obtenerPorNombre("admin").isPresent());
    }

    @Test
    void actualizar_noExiste() {
        when(usuarioRepository.existsById(9L)).thenReturn(false);
        assertNull(usuarioService.actualizar(9L, new Usuario()));
    }

    @Test
    void actualizar_preservaPasswordYVersionSiVacia() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario incoming = new Usuario();
        incoming.setNombre("nuevo");
        incoming.setPassword("");
        Usuario saved = usuarioService.actualizar(1L, incoming);

        assertEquals("old", saved.getPassword());
        assertEquals(3L, saved.getVersion());
        assertEquals(1L, saved.getId());
    }

    @Test
    void actualizar_nuevaPassword() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nueva")).thenReturn("hash-nueva");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario incoming = new Usuario();
        incoming.setPassword("nueva");
        Usuario saved = usuarioService.actualizar(1L, incoming);
        assertEquals("hash-nueva", saved.getPassword());
    }

    @Test
    void actualizar_existsPeroFindVacio() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(usuarioService.actualizar(1L, new Usuario()));
    }

    @Test
    void eliminar_noEncontrado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> usuarioService.eliminar(1L));
    }

    @Test
    void eliminar_conDependencias() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        mockCounts(2L, 0L, 1L, 0L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.eliminar(1L));
        assertTrue(ex.getMessage().contains("dependencias"));
        verify(usuarioRepository, never()).deleteById(1L);
    }

    @Test
    void eliminar_sinDependencias() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        mockCounts(0L);
        usuarioService.eliminar(1L);
        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void resetearPassword_okYNoEncontrado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("123456")).thenReturn("hash-123456");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("hash-123456", usuarioService.resetearPassword(1L).getPassword());

        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> usuarioService.resetearPassword(2L));
    }

    @Test
    void cambiarPassword() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual", "old")).thenReturn(true);
        when(passwordEncoder.encode("nueva")).thenReturn("encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(usuarioService.cambiarPassword(1L, "actual", "nueva"));
        verify(passwordEncoder).encode("nueva");

        usuario.setPassword("old");
        when(passwordEncoder.matches("mala", "old")).thenReturn(false);
        assertFalse(usuarioService.cambiarPassword(1L, "mala", "nueva"));

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(usuarioService.cambiarPassword(99L, "a", "b"));
    }
}
