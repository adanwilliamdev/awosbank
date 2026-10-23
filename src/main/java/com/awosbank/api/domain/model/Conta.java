package com.awosbank.api.domain.model;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoConta;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String numeroConta;

    @Column(nullable = false, length = 10)
    private String agencia;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConta tipoConta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusConta status = StatusConta.ATIVA;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataAbertura = LocalDateTime.now();
}