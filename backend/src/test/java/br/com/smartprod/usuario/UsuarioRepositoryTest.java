package br.com.smartprod.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

// Usa o mesmo H2 (modo PostgreSQL) e as mesmas migrações Flyway do perfil dev.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    @Test
    void salvaEBuscaPorEmailNormalizadoEmMinusculas() {
        repository.saveAndFlush(new Usuario("Maria", "  Maria@Padaria.com ", "hash", Perfil.GERENTE));

        var encontrado = repository.findByEmail("maria@padaria.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getPerfil()).isEqualTo(Perfil.GERENTE);
        assertThat(encontrado.get().isAtivo()).isTrue();
        assertThat(encontrado.get().getCriadoEm()).isNotNull();
    }

    @Test
    void naoPermiteEmailDuplicado() {
        repository.saveAndFlush(new Usuario("Ana", "ana@padaria.com", "hash", Perfil.FUNCIONARIO));

        assertThatThrownBy(() ->
                repository.saveAndFlush(new Usuario("Outra Ana", "ANA@padaria.com", "hash", Perfil.FUNCIONARIO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sabeSeJaExisteUsuarioComOPerfil() {
        repository.deleteAll(); // desfeito no fim do teste, como o resto da transação
        assertThat(repository.existsByPerfil(Perfil.GERENTE)).isFalse();

        repository.saveAndFlush(new Usuario("Maria", "maria@padaria.com", "hash", Perfil.GERENTE));

        assertThat(repository.existsByPerfil(Perfil.GERENTE)).isTrue();
        assertThat(repository.existsByPerfil(Perfil.FUNCIONARIO)).isFalse();
    }

    @Test
    void inativarMantemOUsuarioNoBanco() { // RF002: inativar, nunca remover
        var usuario = repository.saveAndFlush(new Usuario("Beto", "beto@padaria.com", "hash", Perfil.FUNCIONARIO));

        usuario.inativar();
        repository.saveAndFlush(usuario);

        assertThat(repository.findByEmail("beto@padaria.com")).get().extracting(Usuario::isAtivo).isEqualTo(false);
    }
}
