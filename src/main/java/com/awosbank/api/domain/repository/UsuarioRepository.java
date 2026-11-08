package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByCpfCnpj(String cpfCnpj);

    /** Só o ID (sem carregar a entidade e suas associações EAGER): usado pela auditoria. */
    @Query("SELECT u.id FROM Usuario u WHERE u.email = :email")
    Optional<UUID> findIdByEmail(@Param("email") String email);

    // ======== LOGIN: UPDATEs atômicos (sem lock explícito, sem corrida de insert, participam da transação do chamador) ========

    @Query("SELECT u.loginBloqueadoAte FROM Usuario u WHERE u.email = :email")
    Optional<LocalDateTime> bloqueadoAteLogin(@Param("email") String email);

    /** Abre uma janela nova se não há janela, se a anterior venceu ou se um bloqueio anterior já expirou. */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.falhasLogin = 0, u.inicioFalhasLogin = :agora, u.loginBloqueadoAte = NULL
            WHERE u.email = :email
              AND (u.loginBloqueadoAte IS NULL OR u.loginBloqueadoAte <= :agora)
              AND (u.inicioFalhasLogin IS NULL OR u.inicioFalhasLogin < :limite OR u.loginBloqueadoAte IS NOT NULL)
            """)
    int reiniciarJanelaLogin(@Param("email") String email, @Param("agora") LocalDateTime agora,
                             @Param("limite") LocalDateTime limite);

    /** Conta a falha (não conta enquanto bloqueado). */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.falhasLogin = u.falhasLogin + 1
            WHERE u.email = :email AND (u.loginBloqueadoAte IS NULL OR u.loginBloqueadoAte <= :agora)
            """)
    int incrementarFalhaLogin(@Param("email") String email, @Param("agora") LocalDateTime agora);

    /** Aplica o bloqueio se o limite foi atingido. Devolve 1 só na chamada que efetivamente bloqueou. */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.loginBloqueadoAte = :ate
            WHERE u.email = :email AND u.falhasLogin >= :max AND (u.loginBloqueadoAte IS NULL OR u.loginBloqueadoAte <= :agora)
            """)
    int bloquearLogin(@Param("email") String email, @Param("max") int max,
                      @Param("agora") LocalDateTime agora, @Param("ate") LocalDateTime ate);

    /** Senha correta: zera o contador (nunca derruba um bloqueio ainda vigente). */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.falhasLogin = 0, u.inicioFalhasLogin = NULL, u.loginBloqueadoAte = NULL
            WHERE u.email = :email
              AND (u.falhasLogin > 0 OR u.loginBloqueadoAte IS NOT NULL)
              AND (u.loginBloqueadoAte IS NULL OR u.loginBloqueadoAte <= :agora)
            """)
    int limparFalhasLogin(@Param("email") String email, @Param("agora") LocalDateTime agora);

    // ======== SENHA DAS OPERAÇÕES: UPDATEs atômicos (sem lock explícito, sem corrida de insert, participam da transação do chamador) ========

    @Query("SELECT u.senhaOpBloqueadaAte FROM Usuario u WHERE u.email = :email")
    Optional<LocalDateTime> bloqueadoAteSenhaOp(@Param("email") String email);

    /** Abre uma janela nova se não há janela, se a anterior venceu ou se um bloqueio anterior já expirou. */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.falhasSenhaOp = 0, u.inicioFalhasSenhaOp = :agora, u.senhaOpBloqueadaAte = NULL
            WHERE u.email = :email
              AND (u.senhaOpBloqueadaAte IS NULL OR u.senhaOpBloqueadaAte <= :agora)
              AND (u.inicioFalhasSenhaOp IS NULL OR u.inicioFalhasSenhaOp < :limite OR u.senhaOpBloqueadaAte IS NOT NULL)
            """)
    int reiniciarJanelaSenhaOp(@Param("email") String email, @Param("agora") LocalDateTime agora,
                             @Param("limite") LocalDateTime limite);

    /** Conta a falha (não conta enquanto bloqueado). */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.falhasSenhaOp = u.falhasSenhaOp + 1
            WHERE u.email = :email AND (u.senhaOpBloqueadaAte IS NULL OR u.senhaOpBloqueadaAte <= :agora)
            """)
    int incrementarFalhaSenhaOp(@Param("email") String email, @Param("agora") LocalDateTime agora);

    /** Aplica o bloqueio se o limite foi atingido. Devolve 1 só na chamada que efetivamente bloqueou. */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.senhaOpBloqueadaAte = :ate
            WHERE u.email = :email AND u.falhasSenhaOp >= :max AND (u.senhaOpBloqueadaAte IS NULL OR u.senhaOpBloqueadaAte <= :agora)
            """)
    int bloquearSenhaOp(@Param("email") String email, @Param("max") int max,
                      @Param("agora") LocalDateTime agora, @Param("ate") LocalDateTime ate);

    /** Senha correta: zera o contador (nunca derruba um bloqueio ainda vigente). */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Usuario u SET u.falhasSenhaOp = 0, u.inicioFalhasSenhaOp = NULL, u.senhaOpBloqueadaAte = NULL
            WHERE u.email = :email
              AND (u.falhasSenhaOp > 0 OR u.senhaOpBloqueadaAte IS NOT NULL)
              AND (u.senhaOpBloqueadaAte IS NULL OR u.senhaOpBloqueadaAte <= :agora)
            """)
    int limparFalhasSenhaOp(@Param("email") String email, @Param("agora") LocalDateTime agora);
}
