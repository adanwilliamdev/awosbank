package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    Page<Auditoria> findByUsuarioEmailOrderByDataHoraDesc(String usuarioEmail, Pageable pageable);

    long countByUsuarioEmailAndAcao(String usuarioEmail, AcaoAuditoria acao);
}
