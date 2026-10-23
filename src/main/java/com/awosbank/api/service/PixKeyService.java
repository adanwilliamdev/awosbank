package com.awosbank.api.service;

import com.awosbank.api.domain.enums.TipoChavePix;
import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.PixKeyRepository;
import com.awosbank.api.dto.PixKeyDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.util.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PixKeyService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern TELEFONE = Pattern.compile("^\\+?\\d{10,13}$");
    private static final Pattern UUID_RE = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final PixKeyRepository repo;

    public List<PixKey> listar(Usuario u) {
        return repo.findByUsuario(u);
    }

    public PixKey buscarPorChave(String chave) {
        return buscarOpcional(chave)
                .orElseThrow(() -> new BusinessException("Chave PIX não encontrada"));
    }

    /**
     * Busca por uma chave digitada livremente. Como o tipo não é informado, testa as
     * formas normalizadas possíveis (como digitada, minúscula e só dígitos/telefone).
     */
    public Optional<PixKey> buscarOpcional(String chave) {
        if (chave == null || chave.isBlank()) return Optional.empty();
        String bruta = chave.trim();
        Optional<PixKey> r = repo.findByChave(bruta);
        if (r.isEmpty()) r = repo.findByChave(bruta.toLowerCase(Locale.ROOT));
        if (r.isEmpty()) {
            String digitos = bruta.replaceAll("[\\s().-]", "");
            if (!digitos.equals(bruta)) r = repo.findByChave(digitos);
        }
        return r;
    }

    @Transactional
    public PixKey adicionar(Usuario u, PixKeyDTO dto) {
        if (dto == null || dto.getTipo() == null) {
            throw new BusinessException("Tipo da chave é obrigatório");
        }
        if (repo.countByUsuario(u) >= 5) {
            throw new BusinessException("Limite de 5 chaves PIX por conta");
        }
        String chave = normalizarEValidar(u, dto.getTipo(), dto.getChave());
        if (repo.existsByChave(chave)) {
            throw new BusinessException("Chave já cadastrada");
        }
        try {
            return repo.saveAndFlush(PixKey.builder()
                    .usuario(u).tipo(dto.getTipo()).chave(chave).build());
        } catch (DataIntegrityViolationException e) {
            // corrida entre duas requisições com a mesma chave: a constraint UNIQUE decide
            throw new BusinessException("Chave já cadastrada");
        }
    }

    /** Valida o formato conforme o tipo e devolve a forma canônica que será armazenada. */
    String normalizarEValidar(Usuario u, TipoChavePix tipo, String entrada) {
        String valor = entrada == null ? "" : entrada.trim();
        switch (tipo) {
            case CPF -> {
                String cpf = valor.replaceAll("\\D", "");
                if (cpf.length() != 11 || !DocumentoValidator.isValid(cpf)) {
                    throw new BusinessException("CPF inválido");
                }
                if (!cpf.equals(u.getCpfCnpj())) {
                    throw new BusinessException("A chave CPF deve ser o CPF do titular da conta");
                }
                return cpf;
            }
            case EMAIL -> {
                String email = valor.toLowerCase(Locale.ROOT);
                if (email.length() > 120 || !EMAIL.matcher(email).matches()) {
                    throw new BusinessException("E-mail inválido");
                }
                return email;
            }
            case TELEFONE -> {
                String tel = valor.replaceAll("[\\s().-]", "");
                if (!TELEFONE.matcher(tel).matches()) {
                    throw new BusinessException("Telefone inválido (use DDD + número, ex.: 11987654321)");
                }
                return tel;
            }
            case ALEATORIA -> {
                if (valor.isEmpty()) return UUID.randomUUID().toString();
                if (!UUID_RE.matcher(valor).matches()) {
                    throw new BusinessException("Chave aleatória deve ser um UUID válido (ou deixe em branco para gerar)");
                }
                return valor.toLowerCase(Locale.ROOT);
            }
            default -> throw new BusinessException("Tipo de chave não suportado");
        }
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
