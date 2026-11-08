package com.awosbank.api.service;

import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByEmail(UsuarioService.normalizarEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // Bloqueio temporário por excesso de falhas: o Spring Security recusa o login (LockedException)
        // ANTES de conferir a senha, então nem a senha correta entra enquanto durar o bloqueio.
        boolean bloqueada = u.getLoginBloqueadoAte() != null && u.getLoginBloqueadoAte().isAfter(LocalDateTime.now());

        return User.withUsername(u.getEmail())
                .password(u.getSenha())
                .accountLocked(bloqueada)
                .authorities(u.getRoles().toArray(new String[0]))
                .build();
    }
}