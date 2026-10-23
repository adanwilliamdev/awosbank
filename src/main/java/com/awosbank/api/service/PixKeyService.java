package com.awosbank.api.service;

import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.PixKeyRepository;
import com.awosbank.api.dto.PixKeyDTO;
import com.awosbank.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PixKeyService {

    private final PixKeyRepository repo;

    public List<PixKey> listar(Usuario u) {
        return repo.findByUsuario(u);
    }

    public PixKey buscarPorChave(String chave) {
        return repo.findByChave(chave)
                .orElseThrow(() -> new BusinessException("Chave PIX não encontrada"));
    }

    @Transactional
    public PixKey adicionar(Usuario u, PixKeyDTO dto) {
        if (repo.countByUsuario(u) >= 5) {
            throw new BusinessException("Limite de 5 chaves PIX por conta");
        }
        String chave = dto.getChave().trim();
        if (repo.existsByChave(chave)) {
            throw new BusinessException("Chave já cadastrada");
        }
        PixKey k = PixKey.builder()
                .usuario(u).tipo(dto.getTipo()).chave(chave).build();
        return repo.save(k);
    }

    @Transactional
    public void remover(Usuario u, UUID id) {
        PixKey k = repo.findById(id)
                .orElseThrow(() -> new BusinessException("Chave não encontrada"));
        if (!k.getUsuario().getId().equals(u.getId())) {
            throw new BusinessException("Chave não pertence ao usuário");
        }
        repo.delete(k);
    }
}