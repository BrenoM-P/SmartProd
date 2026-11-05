package br.com.smartprod.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import br.com.smartprod.usuario.Perfil;
import br.com.smartprod.usuario.Usuario;
import br.com.smartprod.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private UsuarioDetailsService service;

    @Test
    void gerenteAtivoViraContaHabilitadaComRoleGerente() {
        when(repository.findByEmail("maria@padaria.com"))
                .thenReturn(Optional.of(new Usuario("Maria", "maria@padaria.com", "$hash", Perfil.GERENTE)));

        UserDetails detalhes = service.loadUserByUsername("maria@padaria.com");

        assertThat(detalhes.getUsername()).isEqualTo("maria@padaria.com");
        assertThat(detalhes.getPassword()).isEqualTo("$hash");
        assertThat(detalhes.isEnabled()).isTrue();
        assertThat(detalhes.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_GERENTE");
    }

    @Test
    void funcionarioViraRoleFuncionario() {
        when(repository.findByEmail("beto@padaria.com"))
                .thenReturn(Optional.of(new Usuario("Beto", "beto@padaria.com", "$hash", Perfil.FUNCIONARIO)));

        UserDetails detalhes = service.loadUserByUsername("beto@padaria.com");

        assertThat(detalhes.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_FUNCIONARIO");
    }

    @Test
    void usuarioInativoViraContaDesabilitada() {
        Usuario inativo = new Usuario("Ana", "ana@padaria.com", "$hash", Perfil.FUNCIONARIO);
        inativo.inativar();
        when(repository.findByEmail("ana@padaria.com")).thenReturn(Optional.of(inativo));

        assertThat(service.loadUserByUsername("ana@padaria.com").isEnabled()).isFalse();
    }

    @Test
    void normalizaOEmailAntesDeBuscar() {
        when(repository.findByEmail("maria@padaria.com"))
                .thenReturn(Optional.of(new Usuario("Maria", "maria@padaria.com", "$hash", Perfil.GERENTE)));

        assertThat(service.loadUserByUsername("  Maria@Padaria.COM ").getUsername())
                .isEqualTo("maria@padaria.com");
    }

    @Test
    void emailInexistenteLancaUsernameNotFound() {
        when(repository.findByEmail("ninguem@padaria.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ninguem@padaria.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
