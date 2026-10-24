package com.awosbank.api.domain.model;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro do histórico de auditoria. Append-only: a entidade é {@link Immutable} (o Hibernate nunca
 * emite UPDATE) e, no PostgreSQL, um trigger bloqueia UPDATE/DELETE (ver migration V2).
 */
@Entity
@Immutable
@Table(name = "auditoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, length = 120)
    private String usuarioEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    @Column(nullable = false)
    private boolean sucesso;

    @Column(length = 500)
    private String detalhes;

    @Column(length = 45)
    private String ipOrigem;
}
