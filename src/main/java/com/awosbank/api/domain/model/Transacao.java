package com.awosbank.api.domain.model;

import com.awosbank.api.domain.enums.TipoTransacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacoes")
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
}