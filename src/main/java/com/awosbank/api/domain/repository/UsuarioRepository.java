package com.awosbank.api.domain.repository;

import com.awosbank.api.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByCpfCnpj(String cpfCnpj);

    /** Só o ID (sem carregar a entidade e suas associações EAGER): usado pela auditoria. */
    @Query("SELECT u.id FROM Usuario u WHERE u.email = :email")
    Optional<UUID> findIdByEmail(@Param("email") String email);
}