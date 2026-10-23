package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {
    Optional<Conta> findByNumeroConta(String numeroConta);
    boolean existsByNumeroConta(String numeroConta);
}