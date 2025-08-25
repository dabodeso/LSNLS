package com.lsnls.config;

import com.lsnls.service.JwtService;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("➡️ [JWT FILTER] Entrando en filtro para ruta: {} {}", request.getMethod(), request.getServletPath());
        final String path = request.getServletPath();
        final String authHeader = request.getHeader("Authorization");
        log.info("🔎 [JWT FILTER] Header Authorization recibido: {}", authHeader);
        log.info("🔎 [JWT FILTER] Todas las cabeceras: {}", java.util.Collections.list(request.getHeaderNames()));
        
        // Solo aplicar el filtro a rutas de API
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Si es una ruta pública, permitir sin token
            if ((path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) || 
                isPublicResource(path)) {
                log.debug("✅ Ruta pública, permitiendo acceso: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // Para páginas HTML protegidas y API endpoints
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("❌ [JWT FILTER] Token no proporcionado o inválido para ruta: {}. Header Authorization: {}", path, authHeader);
                
                // Si es una llamada API (no HTML), responder con 401
                if (!path.endsWith(".html")) {
                    log.debug("🚫 Respondiendo 401 para llamada API: {}", path);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token no proporcionado\"}");
                    return;
                }
                // Si es una página HTML, permitir que el auth-guard.js del frontend maneje la redirección
                else {
                    log.debug("📄 Página HTML sin token, permitiendo acceso para que auth-guard.js maneje la redirección: {}", path);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            final String jwt = authHeader.substring(7);
            log.info("🔑 [JWT FILTER] Token extraído: {}", jwt);
            final String username = jwtService.extractUsername(jwt);
            log.info("👤 [JWT FILTER] Username extraído del token: {}", username);
            
            if (username == null) {
                log.error("❌ [JWT FILTER] No se pudo extraer el username del token. Token: {}", jwt);
                handleAuthError(response, "Token inválido");
                return;
            }

            log.info("👤 [JWT FILTER] Token válido para usuario: {}", username);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                log.info("🔐 [JWT FILTER] UserDetails cargado: {}", userDetails.getUsername());
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.info("✅ [JWT FILTER] Token validado correctamente para usuario: {}", username);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.info("✅ [JWT FILTER] Permitiendo acceso a ruta protegida: {}", path);
                    filterChain.doFilter(request, response);
                } else {
                    log.warn("❌ [JWT FILTER] Token expirado o inválido para usuario: {}. Token: {}", username, jwt);
                    handleAuthError(response, "Token expirado");
                }
            } else {
                log.info("🔄 [JWT FILTER] Ya existe autenticación en el contexto para usuario: {}", SecurityContextHolder.getContext().getAuthentication().getName());
                filterChain.doFilter(request, response);
            }
            
        } catch (Exception e) {
            log.error("❌ [JWT FILTER] Error procesando la petición: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            // No marcar como 401 cualquier error de aplicación. Si no es un problema de autenticación,
            // devolver 500 para que el frontend no redirija al login por error ajeno a auth.
            try {
                if (!path.startsWith("/api/")) {
                    handleAuthError(response, e.getMessage());
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Error interno del servidor\"}");
                }
            } catch (IOException ioEx) {
                throw new ServletException(ioEx);
            }
        }
        log.info("⬅️ [JWT FILTER] Saliendo del filtro para ruta: {} {}", request.getMethod(), request.getServletPath());
    }

    private void handleAuthError(HttpServletResponse response, String message) throws IOException {
        log.warn("🚫 Error de autenticación: {}", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Error de autenticación: " + message + "\"}");
    }

    private boolean isPublicResource(String path) {
        boolean isPublic = path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/login.html") ||
               path.equals("/register.html") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.equals("/favicon.ico") ||
               path.equals("/error");
               
        log.debug("🔍 Verificando si {} es público: {}", path, isPublic);
        return isPublic;
    }
} 