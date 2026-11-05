package br.com.smartprod.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Conferência de e-mail e senha: {@link UsuarioDetailsService} + BCrypt (RNF-S001). */
@Configuration
class AuthConfig {

    @Bean
    AuthenticationManager authenticationManager(UsuarioDetailsService usuarioDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(usuarioDetailsService);
        provedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provedor);
    }
}
