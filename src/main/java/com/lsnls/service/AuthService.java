package com.lsnls.service;

import com.lsnls.dto.AuthResponse;
import com.lsnls.dto.LoginRequest;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Maneja el proceso de login
     */
    public AuthResponse login(LoginRequest request) {
        try {
            log.debug("🔍 Validando credenciales para usuario: {}", request.getNombre());
            
            // Autenticar usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getNombre(),
                    request.getPassword()
                )
            );
            
            log.debug("✅ Autenticación exitosa, estableciendo contexto de seguridad");
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Obtener usuario y generar token
            log.debug("🔍 Buscando usuario en base de datos");
            Usuario usuario = usuarioRepository.findByNombre(request.getNombre())
                .orElseThrow(() -> {
                    log.error("❌ Usuario no encontrado en base de datos: {}", request.getNombre());
                    return new RuntimeException("Usuario no encontrado");
                });
            
            log.debug("🔑 Generando token JWT");
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getNombre());
            String jwtToken = jwtService.generateToken(userDetails);
            
            log.info("✅ Login completado exitosamente para: {}", request.getNombre());
            return new AuthResponse(jwtToken, usuario);
        } catch (Exception e) {
            log.error("❌ Error en proceso de login: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            SecurityContextHolder.clearContext();
            throw new RuntimeException("Error en la autenticación: " + e.getMessage());
        }
    }

    /**
     * Maneja el registro de nuevos usuarios
     * PROTEGIDO CONTRA RACE CONDITIONS con manejo de constraints de BD
     */
    public AuthResponse register(Usuario usuario) {
        try {
            // Encriptar contraseña y guardar
            // La BD manejará la constraint UNIQUE para evitar duplicados
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            
            // Generar token
            UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getNombre());
            String jwtToken = jwtService.generateToken(userDetails);
            
            return new AuthResponse(jwtToken, usuarioGuardado);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // La BD detectó un usuario duplicado
            log.warn("❌ Intento de registrar usuario duplicado: {}", usuario.getNombre());
            throw new RuntimeException("El usuario ya existe");
        } catch (Exception e) {
            log.error("❌ Error en registro de usuario: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException("Error interno al registrar usuario: " + e.getMessage());
        }
    }

    /**
     * Obtiene el usuario autenticado actual
     */
    public Optional<Usuario> getCurrentUser() {
        log.debug("👤 Obteniendo usuario actual del contexto de seguridad");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null) {
            log.warn("❌ No hay autenticación en el contexto de seguridad");
            return Optional.empty();
        }
        
        if (!auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            log.warn("❌ Usuario no autenticado o anónimo");
            return Optional.empty();
        }

        log.debug("🔍 Buscando usuario en base de datos: {}", auth.getName());
        Optional<Usuario> usuario = usuarioRepository.findByNombre(auth.getName());
        
        if (usuario.isPresent()) {
            log.debug("✅ Usuario encontrado: {}", usuario.get().getNombre());
        } else {
            log.warn("❌ Usuario no encontrado en base de datos: {}", auth.getName());
        }
        
        return usuario;
    }

    /**
     * Verifica si hay un usuario autenticado
     */
    public boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    /**
     * Cierra la sesión actual
     */
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifica si el token es válido
     */
    public boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return jwtService.isTokenValid(token, userDetails);
        } catch (Exception e) {
            return false;
        }
    }
} 