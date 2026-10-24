package com.awosbank.api.service;

import com.awosbank.api.audit.Auditado;
import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.domain.repository.TransacaoRepository;
import com.awosbank.api.dto.ExtratoFiltroDTO;
import com.awosbank.api.dto.TransacaoView;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PixKeyService pixKeyService;

    @PersistenceContext
    private EntityManager em;

    private static final BigDecimal LIMITE_OPERACAO = new BigDecimal("1000000.00");

    /** Transferência informando o número da conta de destino (formulário web). */
    @Auditado(AcaoAuditoria.TRANSFERENCIA)
    @Transactional
    public Transacao transferir(Usuario usuario, TransferenciaDTO dto) {
        validarSenha(usuario, dto.getSenha());
        Conta destino = contaRepository.findByNumeroConta(normalizar(dto.getNumeroContaDestino()))
                .orElseThrow(() -> new BusinessException("Conta destino não encontrada"));
        return executarTransferencia(usuario, destino.getId(), dto.getValor(), dto.getTipo());
    }

    /** Transferência por chave PIX ou número de conta (API). */
    @Auditado(AcaoAuditoria.TRANSFERENCIA)
    @Transactional
    public Transacao transferirPorIdentificador(Usuario usuario, TransferenciaDTO dto) {
        validarSenha(usuario, dto.getSenha());
        Conta destino = resolverDestino(dto.getNumeroContaDestino());
        return executarTransferencia(usuario, destino.getId(), dto.getValor(), dto.getTipo());
    }

    @Auditado(AcaoAuditoria.DEPOSITO)
    @Transactional
    public Transacao depositar(Usuario usuario, BigDecimal valor) {
        BigDecimal v = validarValor(valor, "Valor inválido para depósito");
        Conta conta = lockar(usuario.getConta().getId());
        if (conta.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta não está ativa");
        }
        conta.setSaldo(conta.getSaldo().add(v));
        contaRepository.save(conta);
        return registrar(null, conta, v, TipoTransacao.DEPOSITO);
    }

    @Auditado(AcaoAuditoria.SAQUE)
    @Transactional
    public Transacao sacar(Usuario usuario, BigDecimal valor) {
        BigDecimal v = validarValor(valor, "Valor inválido para saque");
        Conta conta = lockar(usuario.getConta().getId());
        if (conta.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta não está ativa");
        }
        if (conta.getSaldo().compareTo(v) < 0) {
            throw new BusinessException("Saldo insuficiente");
        }
        conta.setSaldo(conta.getSaldo().subtract(v));
        contaRepository.save(conta);
        return registrar(conta, null, v, TipoTransacao.SAQUE);
    }

    /**
     * Núcleo único da transferência. Bloqueia as DUAS contas com lock pessimista
     * sempre em ordem crescente de ID (evita deadlock em transferências cruzadas
     * A→B e B→A) e só então valida status e saldo sobre os dados frescos do banco.
     */
    private Transacao executarTransferencia(Usuario usuario, UUID destinoId,
                                            BigDecimal valorInformado, TipoTransacao tipo) {
        if (tipo != TipoTransacao.PIX && tipo != TipoTransacao.TED) {
            throw new BusinessException("Tipo de transferência inválido");
        }
        BigDecimal valor = validarValor(valorInformado, "Valor deve ser maior que zero");

        UUID origemId = usuario.getConta().getId();
        if (destinoId.equals(origemId)) {
            throw new BusinessException("Não é possível transferir para a própria conta");
        }

        boolean origemPrimeiro = origemId.compareTo(destinoId) < 0;
        Conta primeira = lockar(origemPrimeiro ? origemId : destinoId);
        Conta segunda = lockar(origemPrimeiro ? destinoId : origemId);
        Conta origem = origemPrimeiro ? primeira : segunda;
        Conta destino = origemPrimeiro ? segunda : primeira;

        if (origem.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta origem não está ativa");
        }
        if (destino.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta destino não está ativa");
        }
        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new BusinessException("Saldo insuficiente");
        }

        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));
        contaRepository.save(origem);
        contaRepository.save(destino);
        return registrar(origem, destino, valor, tipo);
    }

    private Conta lockar(UUID contaId) {
        Conta conta = contaRepository.findByIdForUpdate(contaId)
                .orElseThrow(() -> new BusinessException("Conta não encontrada"));
        // Se a conta já estava no contexto de persistência (ex.: carregada por findByNumeroConta antes do lock),
        // o Hibernate devolve a instância em cache, com o saldo de ANTES da espera pelo lock => atualização perdida.
        // Recarregar com o lock já adquirido garante o saldo mais recente commitado.
        em.refresh(conta);
        return conta;
    }

    private void validarSenha(Usuario usuario, String senha) {
        if (senha == null || !passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new BusinessException("Senha incorreta");
        }
    }

    /** Valor positivo, no máximo 2 casas decimais e dentro do limite por operação. */
    private BigDecimal validarValor(BigDecimal valor, String mensagem) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(mensagem);
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("Valor deve ter no máximo 2 casas decimais");
        }
        if (valor.compareTo(LIMITE_OPERACAO) > 0) {
            throw new BusinessException("Valor acima do limite por operação (R$ 1.000.000,00)");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private Transacao registrar(Conta origem, Conta destino, BigDecimal valor, TipoTransacao tipo) {
        return transacaoRepository.save(Transacao.builder()
                .contaOrigem(origem)
                .contaDestino(destino)
                .valor(valor)
                .tipo(tipo)
                .dataHora(LocalDateTime.now())
                .comprovanteCodigo(gerarComprovante())
                .build());
    }

    private String normalizar(String s) {
        return s == null ? "" : s.trim();
    }

    /** Resolve o destino: tenta chave PIX; se não achar, tenta número de conta. */
    private Conta resolverDestino(String identificador) {
        String id = normalizar(identificador);
        return pixKeyService.buscarOpcional(id)
                .map(pk -> pk.getUsuario().getConta())
                .or(() -> contaRepository.findByNumeroConta(id))
                .orElseThrow(() -> new BusinessException("Destinatário não encontrado (chave PIX ou conta)"));
    }

    /** Dados para gráfico (últimos 6 meses) */
    public com.awosbank.api.dto.ChartDataDTO chartData(UUID contaId) {
        LocalDateTime inicio = LocalDateTime.now().minusMonths(5)
                .withDayOfMonth(1).toLocalDate().atStartOfDay();
        List<Transacao> lista = transacaoRepository.buscarExtrato(
                contaId, inicio, LocalDateTime.now(), null);

        java.util.LinkedHashMap<String, BigDecimal[]> meses = new java.util.LinkedHashMap<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM/yyyy");
        for (int i = 5; i >= 0; i--) {
            meses.put(LocalDateTime.now().minusMonths(i).format(fmt),
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Transacao t : lista) {
            String k = t.getDataHora().format(fmt);
            if (!meses.containsKey(k)) continue;
            BigDecimal[] v = meses.get(k);
            boolean entrada = t.getTipo() == TipoTransacao.DEPOSITO
                    || (t.getContaDestino() != null && t.getContaDestino().getId().equals(contaId));
            if (entrada) v[0] = v[0].add(t.getValor());
            else v[1] = v[1].add(t.getValor());
        }
        var labels = new java.util.ArrayList<String>();
        var entradas = new java.util.ArrayList<BigDecimal>();
        var saidas = new java.util.ArrayList<BigDecimal>();
        meses.forEach((k, v) -> {
            labels.add(k); entradas.add(v[0]); saidas.add(v[1]);
        });
        return new com.awosbank.api.dto.ChartDataDTO(labels, entradas, saidas);
    }

    public List<TransacaoView> buscarExtrato(UUID contaId, ExtratoFiltroDTO filtro) {
        LocalDateTime inicio = filtro.getDataInicio() != null
                ? filtro.getDataInicio().atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime fim = filtro.getDataFim() != null
                ? filtro.getDataFim().atTime(LocalTime.MAX)
                : LocalDateTime.now();

        TipoTransacao tipo = (filtro.getTipo() != null && !filtro.getTipo().isBlank())
                ? TipoTransacao.valueOf(filtro.getTipo())
                : null;

        List<Transacao> lista = transacaoRepository.buscarExtrato(contaId, inicio, fim, tipo);
        return lista.stream().map(t -> toView(t, contaId)).toList();
    }

    public List<TransacaoView> ultimasTransacoes(UUID contaId, int limite) {
        return transacaoRepository.buscarUltimas(contaId, PageRequest.of(0, limite))
                .stream().map(t -> toView(t, contaId)).toList();
    }

    private TransacaoView toView(Transacao t, UUID contaId) {
        boolean entrada;
        String descricao;
        if (t.getTipo() == TipoTransacao.DEPOSITO) {
            entrada = true;
            descricao = "Depósito";
        } else if (t.getTipo() == TipoTransacao.SAQUE) {
            entrada = false;
            descricao = "Saque";
        } else {
            entrada = t.getContaDestino() != null && t.getContaDestino().getId().equals(contaId);
            Conta contraparte = entrada ? t.getContaOrigem() : t.getContaDestino();
            String num = contraparte != null ? contraparte.getNumeroConta() : "-";
            descricao = (entrada ? "Recebido de " : "Enviado para ") + "conta " + num;
        }
        return new TransacaoView(t, entrada, descricao);
    }

    public Transacao buscarPorComprovante(String codigo) {
        return transacaoRepository.findByComprovanteCodigo(codigo)
                .orElseThrow(() -> new BusinessException("Comprovante não encontrado"));
    }

    private String gerarComprovante() {
        return "CMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}