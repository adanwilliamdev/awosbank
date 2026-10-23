package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    Optional<Transacao> findByComprovanteCodigo(String comprovanteCodigo);

    @Query("""
            SELECT t FROM Transacao t
            WHERE (t.contaOrigem.id = :contaId OR t.contaDestino.id = :contaId)
              AND t.dataHora BETWEEN :inicio AND :fim
              AND (:tipo IS NULL OR t.tipo = :tipo)
            ORDER BY t.dataHora DESC
            """)
    List<Transacao> buscarExtrato(@Param("contaId") UUID contaId,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  @Param("tipo") TipoTransacao tipo);

    @Query("""
            SELECT t FROM Transacao t
            WHERE (t.contaOrigem.id = :contaId OR t.contaDestino.id = :contaId)
            ORDER BY t.dataHora DESC
            """)
    List<Transacao> buscarUltimas(@Param("contaId") UUID contaId,
                                  org.springframework.data.domain.Pageable pageable);
}