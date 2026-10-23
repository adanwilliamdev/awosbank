package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {
    List<PixKey> findByUsuario(Usuario usuario);
    Optional<PixKey> findByChave(String chave);
    boolean existsByChave(String chave);
    void deleteByChaveAndUsuario(String chave, Usuario usuario);
    long countByUsuario(Usuario usuario);
}