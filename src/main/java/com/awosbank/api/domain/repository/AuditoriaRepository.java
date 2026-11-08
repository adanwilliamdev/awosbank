package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    /**
     * Histórico de UM usuário, identificado pelo ID estável. Nunca consulte o histórico por e-mail:
     * o e-mail é liberado no encerramento da conta e pode ser reutilizado por outra pessoa.
     *
     * <p>Atenção: com {@code usuarioId == null} o Spring Data geraria {@code IS NULL} e devolveria
     * os registros sem dono — quem chama deve garantir que o ID não é nulo (ver {@code AuditoriaService}).
     */
    Page<Auditoria> findByUsuarioIdOrderByDataHoraDesc(UUID usuarioId, Pageable pageable);

    /** Contagem por e-mail do ator (inclui tentativas de login de e-mails sem usuário). Uso interno/testes. */
    long countByUsuarioEmailAndAcao(String usuarioEmail, AcaoAuditoria acao);
}
