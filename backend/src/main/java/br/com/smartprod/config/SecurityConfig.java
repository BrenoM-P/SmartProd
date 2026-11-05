package br.com.smartprod.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.smartprod.auth.UsuarioJwtConverter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * API stateless: sem sessão e sem CSRF, pois a autenticação é por token JWT (RF001).
 *
 * <p>O login ({@code POST /api/auth/login}) e a documentação da API são públicos. Todo o resto exige
 * um token válido no cabeçalho {@code Authorization: Bearer <token>}: sem token, ou com token inválido,
 * vencido, de outro emissor ou de um usuário inexistente ou inativo, a resposta é 401. O perfil é lido
 * do banco a cada requisição e vira a autoridade {@code ROLE_GERENTE} ou {@code ROLE_FUNCIONARIO}
 * (ver {@code UsuarioJwtConverter}), então inativar um usuário ou mudar o perfil vale na hora.
 *
 * <p><b>Como proteger um endpoint (todas as duplas):</b> este arquivo não precisa mudar. Anote o método
 * do controller; com token válido mas perfil sem permissão, a resposta é 403:
 *
 * <pre>
 * &#64;PreAuthorize("hasRole('GERENTE')")                    // só Gerente (custos, dashboard, cadastros)
 * &#64;PreAuthorize("hasAnyRole('GERENTE', 'FUNCIONARIO')")   // qualquer usuário logado
 * </pre>
 *
 * Método sem anotação exige apenas estar logado. Nos testes com MockMvc, use
 * {@code @WithMockUser(roles = "GERENTE")} (ou {@code "FUNCIONARIO"}).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** RNF-S001: BCrypt com no mínimo 10 rounds; o projeto usa 12. */
    static final int BCRYPT_STRENGTH = 12;

    private static final String CAMINHO_DO_LOGIN = "/api/auth/login";
    private static final BearerTokenResolver RESOLVEDOR_PADRAO = new DefaultBearerTokenResolver();

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioJwtConverter conversorDeJwt)
            throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(SecurityConfig::resolverToken)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeJwt)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, CAMINHO_DO_LOGIN).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * No login o cabeçalho Authorization é ignorado: um token antigo ou vencido ainda guardado no
     * navegador não pode impedir a pessoa de entrar de novo.
     */
    static String resolverToken(HttpServletRequest requisicao) {
        if (CAMINHO_DO_LOGIN.equals(requisicao.getRequestURI())) {
            return null;
        }
        return RESOLVEDOR_PADRAO.resolve(requisicao);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
