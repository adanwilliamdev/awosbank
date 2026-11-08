package com.awosbank.api.domain.model;

import com.awosbank.api.domain.enums.TipoTransacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_transacoes_origem_chave",
                columnNames = {"conta_origem_id", "chave_idempotencia"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_origem_id")
    private Conta contaOrigem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_destino_id")
    private Conta contaDestino;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(nullable = false, unique = true, length = 40)
    private String comprovanteCodigo;

    /**
     * Chave enviada pelo cliente (Idempotency-Key / token do formulário). Nula em depósitos, saques e
     * transferências sem chave. Única por conta de origem: repetir a chave não cria outra transação.
     */
    @Column(name = "chave_idempotencia", length = 64)
    private String chaveIdempotencia;
}