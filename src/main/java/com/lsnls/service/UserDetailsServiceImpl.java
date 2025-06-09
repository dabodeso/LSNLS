package com.lsnls.service;

import com.lsnls.entity.Usuario;
import com.lsnls.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNombre(username);
        Usuario usuario = usuarioOpt.orElseThrow(() -> 
            new UsernameNotFoundException("Usuario no encontrado: " + username));

        String authority = usuario.getRol().toString();
        log.debug("🔐 Cargando autoridades para usuario {}: {}", username, authority);

        return User.builder()
                .username(usuario.getNombre())
                .password(usuario.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(authority)))
                .build();
    }
} 