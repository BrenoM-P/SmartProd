package br.com.smartprod.insumo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findAllByOrderByNomeAsc();

    List<Insumo> findByAtivoTrueOrderByNomeAsc();
}
