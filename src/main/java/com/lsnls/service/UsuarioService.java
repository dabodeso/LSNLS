package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;

@Service
@Transactional
public class UsuarioService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    public Usuario crear(Usuario usuario) {
        // Asignar automáticamente la contraseña por defecto
        usuario.setPassword("123456");
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> obtenerPorNombre(String nombre) {
        return usuarioRepository.findByNombre(nombre);
    }

    public Usuario actualizar(Long id, Usuario usuario) {
        if (usuarioRepository.existsById(id)) {
            usuario.setId(id);
            // TEMPORAL: Para desarrollo no encriptar contraseñas (usar texto plano)
            // Si la contraseña se está actualizando, NO encriptarla
            if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
                // Si no se proporciona nueva contraseña, mantener la existente
                Usuario usuarioExistente = usuarioRepository.findById(id).orElse(null);
                if (usuarioExistente != null) {
                    usuario.setPassword(usuarioExistente.getPassword());
                }
            }
            return usuarioRepository.save(usuario);
        }
        return null;
    }

    public void eliminar(Long id) {
        // Verificar que el usuario existe
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario con ID " + id + " no encontrado");
        }

        Usuario usuario = usuarioOpt.get();

        // Verificar dependencias - contar entidades creadas por este usuario
        StringBuilder dependencias = new StringBuilder();
        
        // Verificar preguntas creadas por este usuario
        Long preguntasCount = entityManager.createQuery(
            "SELECT COUNT(p) FROM Pregunta p WHERE p.creacionUsuario.id = :usuarioId", Long.class)
            .setParameter("usuarioId", id)
            .getSingleResult();
        
        if (preguntasCount > 0) {
            dependencias.append("- ").append(preguntasCount).append(" pregunta(s)\n");
        }

        // Verificar cuestionarios creados por este usuario
        Long cuestionariosCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Cuestionario c WHERE c.creacionUsuario.id = :usuarioId", Long.class)
            .setParameter("usuarioId", id)
            .getSingleResult();
        
        if (cuestionariosCount > 0) {
            dependencias.append("- ").append(cuestionariosCount).append(" cuestionario(s)\n");
        }

        // Verificar combos creados por este usuario
        Long combosCount = entityManager.createQuery(
            "SELECT COUNT(c) FROM Combo c WHERE c.creacionUsuario.id = :usuarioId", Long.class)
            .setParameter("usuarioId", id)
            .getSingleResult();
        
        if (combosCount > 0) {
            dependencias.append("- ").append(combosCount).append(" combo(s)\n");
        }

        // Verificar verificaciones de preguntas hechas por este usuario
        Long verificacionesCount = entityManager.createQuery(
            "SELECT COUNT(p) FROM Pregunta p WHERE p.verificacionUsuario.id = :usuarioId", Long.class)
            .setParameter("usuarioId", id)
            .getSingleResult();
        
        if (verificacionesCount > 0) {
            dependencias.append("- ").append(verificacionesCount).append(" verificación(es) de preguntas\n");
        }

        // Si hay dependencias, no permitir la eliminación
        if (dependencias.length() > 0) {
            throw new IllegalArgumentException("No se puede eliminar el usuario '" + usuario.getNombre() + 
                "' porque tiene las siguientes dependencias:\n" + dependencias.toString() + 
                "Reasigna estos elementos a otro usuario antes de eliminar.");
        }

        // Si llegamos aquí, es seguro eliminar
        usuarioRepository.deleteById(id);
    }

    public Usuario resetearPassword(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setPassword("123456");
            return usuarioRepository.save(usuario);
        }
        throw new IllegalArgumentException("Usuario no encontrado");
    }

    public boolean cambiarPassword(Long id, String actual, String nueva) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            // Si las contraseñas están encriptadas, usar passwordEncoder.matches
            boolean coincide = passwordEncoder.matches(actual, usuario.getPassword());
            // Si no están encriptadas, usar equals (descomentar para desarrollo)
            // boolean coincide = usuario.getPassword().equals(actual);
            if (coincide) {
                usuario.setPassword(passwordEncoder.encode(nueva));
                usuarioRepository.save(usuario);
                return true;
            }
        }
        return false;
    }
} 