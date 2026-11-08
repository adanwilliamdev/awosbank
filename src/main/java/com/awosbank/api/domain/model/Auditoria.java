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
 *
 * <p>O dono do registro é identificado por {@link #usuarioId} (UUID, nunca reaproveitado) — e NÃO pelo
 * e-mail, que é liberado quando a conta é encerrada e pode ser reutilizado por outra pessoa.
 * {@link #usuarioEmail} fica apenas como informação do momento do evento (migration V3).
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

    /** Dono do registro. Sem FK: o usuário pode ser excluído (encerramento) e o registro permanece. Nulo se o ator não é um usuário cadastrado. */
    @Column(name = "usuario_id")
    private UUID usuarioId;

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
