package com.awosbank.api.service;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.domain.repository.TransacaoRepository;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransacaoServiceTest {

    // IDs fixos: LOW < HIGH em UUID.compareTo
    private static final UUID LOW = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID HIGH = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock TransacaoRepository transacaoRepository;
    @Mock ContaRepository contaRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PixKeyService pixKeyService;

    TransacaoService service;

    @BeforeEach
    void setUp() {
        service = new TransacaoService(transacaoRepository, contaRepository, passwordEncoder, pixKeyService);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Conta conta(UUID id, String numero, String saldo, StatusConta status) {
        Conta c = Conta.builder().id(id).numeroConta(numero).agencia("0001")
                .saldo(new BigDecimal(saldo)).tipoConta(TipoConta.CORRENTE).status(status).build();
        when(contaRepository.findByIdForUpdate(id)).thenReturn(Optional.of(c));
        when(contaRepository.findByNumeroConta(numero)).thenReturn(Optional.of(c));
        return c;
    }

    private Usuario donoDe(Conta c) {
        return Usuario.builder().senha("hash").conta(c).build();
    }

    private TransferenciaDTO dto(String contaDestino, String valor) {
        TransferenciaDTO d = new TransferenciaDTO();
        d.setNumeroContaDestino(contaDestino);
        d.setValor(new BigDecimal(valor));
        d.setTipo(TipoTransacao.PIX);
        d.setSenha("123456");
        return d;
    }

    private String mensagem(Runnable r) {
        return assertThrows(BusinessException.class, r::run).getMessage();
    }

    @Test
    void transfereEAtualizaOsDoisSaldos() {
        Conta origem = conta(LOW, "111111-1", "500.00", StatusConta.ATIVA);
        Conta destino = conta(HIGH, "222222-2", "10.00", StatusConta.ATIVA);

        Transacao t = service.transferir(donoDe(origem), dto("222222-2", "150.50"));

        assertEquals(new BigDecimal("349.50"), origem.getSaldo());
        assertEquals(new BigDecimal("160.50"), destino.getSaldo());
        assertEquals(new BigDecimal("150.50"), t.getValor());
        assertNotNull(t.getComprovanteCodigo());
    }

    @Test
    void saldoInsuficienteNaoAlteraNadaENaoRegistraTransacao() {
        Conta origem = conta(LOW, "111111-1", "50.00", StatusConta.ATIVA);
        Conta destino = conta(HIGH, "222222-2", "0.00", StatusConta.ATIVA);

        assertEquals("Saldo insuficiente",
                mensagem(() -> service.transferir(donoDe(origem), dto("222222-2", "100.00"))));

        assertEquals(new BigDecimal("50.00"), origem.getSaldo());
        assertEquals(new BigDecimal("0.00"), destino.getSaldo());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void naoPermiteTransferirParaAPropriaConta() {
        Conta origem = conta(LOW, "111111-1", "500.00", StatusConta.ATIVA);
        assertTrue(mensagem(() -> service.transferir(donoDe(origem), dto("111111-1", "10.00")))
                .contains("própria conta"));
    }

    @Test
    void senhaIncorretaEhRejeitadaAntesDeQualquerLock() {
        Conta origem = conta(LOW, "111111-1", "500.00", StatusConta.ATIVA);
        conta(HIGH, "222222-2", "0.00", StatusConta.ATIVA);
        TransferenciaDTO d = dto("222222-2", "10.00");
        d.setSenha("errada");

        assertEquals("Senha incorreta", mensagem(() -> service.transferir(donoDe(origem), d)));
        verify(contaRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void contaDestinoInativaEhRejeitada() {
        Conta origem = conta(LOW, "111111-1", "500.00", StatusConta.ATIVA);
        conta(HIGH, "222222-2", "0.00", StatusConta.BLOQUEADA);
        assertTrue(mensagem(() -> service.transferir(donoDe(origem), dto("222222-2", "10.00")))
                .contains("destino não está ativa"));
    }

    @Test
    void rejeitaValorZeroNegativoOuComMaisDeDuasCasas() {
        Conta origem = conta(LOW, "111111-1", "500.00", StatusConta.ATIVA);
        conta(HIGH, "222222-2", "0.00", StatusConta.ATIVA);
        for (String invalido : new String[]{"0", "-5.00", "10.005"}) {
            assertThrows(BusinessException.class,
                    () -> service.transferir(donoDe(origem), dto("222222-2", invalido)),
                    "valor " + invalido + " deveria ser rejeitado");
        }
    }

    @Test
    void rejeitaValorAcimaDoLimiteDeOperacao() {
        Conta origem = conta(LOW, "111111-1", "9999999.00", StatusConta.ATIVA);
        conta(HIGH, "222222-2", "0.00", StatusConta.ATIVA);
        assertTrue(mensagem(() -> service.transferir(donoDe(origem), dto("222222-2", "1000000.01")))
                .contains("limite"));
    }

    @Test
    void travaSempreAContaDeMenorIdPrimeiro_origemMaior() {
        Conta origem = conta(HIGH, "222222-2", "100.00", StatusConta.ATIVA);
        conta(LOW, "111111-1", "0.00", StatusConta.ATIVA);

        service.transferir(donoDe(origem), dto("111111-1", "10.00"));

        InOrder ordem = inOrder(contaRepository);
        ordem.verify(contaRepository).findByIdForUpdate(LOW);
        ordem.verify(contaRepository).findByIdForUpdate(HIGH);
    }

    @Test
    void travaSempreAContaDeMenorIdPrimeiro_origemMenor() {
        Conta origem = conta(LOW, "111111-1", "100.00", StatusConta.ATIVA);
        conta(HIGH, "222222-2", "0.00", StatusConta.ATIVA);

        service.transferir(donoDe(origem), dto("222222-2", "10.00"));

        InOrder ordem = inOrder(contaRepository);
        ordem.verify(contaRepository).findByIdForUpdate(LOW);
        ordem.verify(contaRepository).findByIdForUpdate(HIGH);
    }

    @Test
    void saqueNaoPodeDeixarSaldoNegativo() {
        Conta c = conta(LOW, "111111-1", "20.00", StatusConta.ATIVA);
        assertEquals("Saldo insuficiente", mensagem(() -> service.sacar(donoDe(c), new BigDecimal("20.01"))));
        assertEquals(new BigDecimal("20.00"), c.getSaldo());
    }

    @Test
    void depositoEmContaInativaEhRejeitado() {
        Conta c = conta(LOW, "111111-1", "20.00", StatusConta.BLOQUEADA);
        assertThrows(BusinessException.class, () -> service.depositar(donoDe(c), new BigDecimal("10.00")));
    }
}
