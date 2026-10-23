package com.awosbank.api.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PixKeyService pixKeyService;   // ← ESTA LINHA FALTAVA

    @Transactional
    public Transacao transferir(Usuario usuario, TransferenciaDTO dto) {
        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new BusinessException("Senha incorreta");
        }
        Conta origem = usuario.getConta();
        if (origem.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta origem não está ativa");
        }
        Conta destino = contaRepository.findByNumeroConta(dto.getNumeroContaDestino())
                .orElseThrow(() -> new BusinessException("Conta destino não encontrada"));
        if (destino.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta destino não está ativa");
        }
        if (destino.getId().equals(origem.getId())) {
            throw new BusinessException("Não é possível transferir para a própria conta");
        }
        if (dto.getTipo() != TipoTransacao.PIX && dto.getTipo() != TipoTransacao.TED) {
            throw new BusinessException("Tipo de transferência inválido");
        }

        BigDecimal valor = dto.getValor();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor deve ser maior que zero");
        }
        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new BusinessException("Saldo insuficiente");
        }

        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));
        contaRepository.save(origem);
        contaRepository.save(destino);

        Transacao t = Transacao.builder()
                .contaOrigem(origem)
                .contaDestino(destino)
                .valor(valor)
                .tipo(dto.getTipo())
                .dataHora(LocalDateTime.now())
                .comprovanteCodigo(gerarComprovante())
                .build();
        return transacaoRepository.save(t);
    }

    @Transactional
    public Transacao depositar(Usuario usuario, BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor inválido para depósito");
        }
        Conta conta = usuario.getConta();
        conta.setSaldo(conta.getSaldo().add(valor));
        contaRepository.save(conta);

        Transacao t = Transacao.builder()
                .contaDestino(conta)
                .valor(valor)
                .tipo(TipoTransacao.DEPOSITO)
                .dataHora(LocalDateTime.now())
                .comprovanteCodigo(gerarComprovante())
                .build();
        return transacaoRepository.save(t);
    }

    @Transactional
    public Transacao sacar(Usuario usuario, BigDecimal valor) {
        Conta conta = usuario.getConta();
        if (conta.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta não está ativa");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor inválido para saque");
        }
        if (conta.getSaldo().compareTo(valor) < 0) {
            throw new BusinessException("Saldo insuficiente");
        }
        conta.setSaldo(conta.getSaldo().subtract(valor));
        contaRepository.save(conta);

        Transacao t = Transacao.builder()
                .contaOrigem(conta)
                .valor(valor)
                .tipo(TipoTransacao.SAQUE)
                .dataHora(LocalDateTime.now())
                .comprovanteCodigo(gerarComprovante())
                .build();
        return transacaoRepository.save(t);
    }

    /** Resolve destino: tenta PIX key; se não achar, tenta número de conta. */
    private Conta resolverDestino(String identificador) {
        String id = identificador.trim();
        try {
            var pk = pixKeyService.buscarPorChave(id);
            if (pk.getUsuario() != null && pk.getUsuario().getConta() != null) {
                return pk.getUsuario().getConta();
            }
        } catch (Exception ignored) { }
        return contaRepository.findByNumeroConta(id)
                .orElseThrow(() -> new BusinessException("Destinatário não encontrado (chave PIX ou conta)"));
    }

    /** Transferência por chave PIX ou conta */
    @Transactional
    public Transacao transferirPorIdentificador(Usuario usuario, TransferenciaDTO dto) {
        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new BusinessException("Senha incorreta");
        }
        Conta origem = usuario.getConta();
        if (origem.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta origem não está ativa");
        }
        Conta destino = resolverDestino(dto.getNumeroContaDestino());
        if (destino.getStatus() != StatusConta.ATIVA) {
            throw new BusinessException("Conta destino não está ativa");
        }
        if (destino.getId().equals(origem.getId())) {
            throw new BusinessException("Não é possível transferir para a própria conta");
        }
        BigDecimal valor = dto.getValor();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor deve ser maior que zero");
        }
        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new BusinessException("Saldo insuficiente");
        }
        origem.setSaldo(origem.getSaldo().subtract(valor));
        destino.setSaldo(destino.getSaldo().add(valor));
        contaRepository.save(origem);
        contaRepository.save(destino);

        Transacao t = Transacao.builder()
                .contaOrigem(origem).contaDestino(destino).valor(valor)
                .tipo(dto.getTipo()).dataHora(LocalDateTime.now())
                .comprovanteCodigo(gerarComprovante()).build();
        return transacaoRepository.save(t);
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