package br.com.smartprod.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** O e-mail é sempre gravado em minúsculas (ver construtor de {@link Usuario}). */
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Para a edição: o e-mail pode já ser do próprio usuário, mas não de outro. */
    boolean existsByEmailAndIdNot(String email, Long id);

    List<Usuario> findAllByOrderByNomeAsc();

    List<Usuario> findByAtivoOrderByNomeAsc(boolean ativo);
}
