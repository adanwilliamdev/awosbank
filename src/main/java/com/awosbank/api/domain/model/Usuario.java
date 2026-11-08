package com.awosbank.api.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code @DynamicUpdate}: o UPDATE só inclui as colunas alteradas. Os contadores de força bruta são
 * atualizados por UPDATE direto no banco; sem isto, salvar o perfil com uma cópia antiga em memória
 * sobrescreveria um contador/bloqueio recém-gravado.
 */
@Entity
@DynamicUpdate
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 20)
    private String cpfCnpj;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(length = 20)
    private String telefone;

    @Column(length = 20)
    private String dataNascimento; // formato yyyy-MM-dd

    @Column(length = 200)
    private String endereco;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "conta_id", referencedColumnName = "id")
    private Conta conta;

    // ---- Proteção contra força bruta (ver ProtecaoForcaBrutaService; atualizados por UPDATE atômico) ----
    @Column(name = "falhas_login", nullable = false)
    private int falhasLogin;

    @Column(name = "inicio_falhas_login")
    private LocalDateTime inicioFalhasLogin;

    @Column(name = "login_bloqueado_ate")
    private LocalDateTime loginBloqueadoAte;

    @Column(name = "falhas_senha_op", nullable = false)
    private int falhasSenhaOp;

    @Column(name = "inicio_falhas_senha_op")
    private LocalDateTime inicioFalhasSenhaOp;

    @Column(name = "senha_op_bloqueada_ate")
    private LocalDateTime senhaOpBloqueadaAte;

    /** Retorna as iniciais do usuário: "Cliente de Teste" → "CT" */
    public String getIniciais() {
        if (nomeCompleto == null || nomeCompleto.isBlank()) return "?";
        String[] partes = nomeCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    /** Retorna o primeiro nome: "Cliente de Teste" → "Cliente" */
    public String getPrimeiroNome() {
        if (nomeCompleto == null || nomeCompleto.isBlank()) return "";
        return nomeCompleto.trim().split("\\s+")[0];
    }
}