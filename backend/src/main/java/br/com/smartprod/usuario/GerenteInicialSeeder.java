package br.com.smartprod.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro Gerente na subida da aplicação (RF001), para alguém conseguir entrar.
 *
 * <p>Só age quando {@code app.seed.gerente.email} e {@code .senha} estão preenchidos: o perfil dev já
 * traz valores de exemplo e, em produção, vêm de variáveis de ambiente. Se o e-mail já existe, não faz
 * nada (nunca sobrescreve uma senha). A senha é gravada com BCrypt e nunca vai para o log.
 */
@Component
@EnableConfigurationProperties(SeedGerenteProperties.class)
class GerenteInicialSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GerenteInicialSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedGerenteProperties propriedades;

    GerenteInicialSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            SeedGerenteProperties propriedades) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.propriedades = propriedades;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!propriedades.preenchido()) {
            log.debug("Seed do Gerente inicial desativada: e-mail e senha não informados.");
            return;
        }
        String email = Usuario.normalizarEmail(propriedades.email());
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        usuarioRepository.save(new Usuario(propriedades.nome(), email,
                passwordEncoder.encode(propriedades.senha()), Perfil.GERENTE));
        log.info("Gerente inicial criado pela seed.");
    }
}
