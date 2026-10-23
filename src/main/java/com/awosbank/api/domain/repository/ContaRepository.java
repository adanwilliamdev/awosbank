package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.model.Conta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {
    Optional<Conta> findByNumeroConta(String numeroConta);
    boolean existsByNumeroConta(String numeroConta);

    /**
     * Carrega a conta com lock pessimista de escrita (SELECT ... FOR UPDATE).
     * Deve ser chamado dentro de uma transação: serializa as operações que
     * alteram o saldo e evita lost update / double spend.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conta c WHERE c.id = :id")
    Optional<Conta> findByIdForUpdate(@Param("id") UUID id);
}
