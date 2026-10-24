package com.awosbank.api.service;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ContaServiceTest {

    private ContaRepository repository;
    private ContaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ContaRepository.class);
        when(repository.save(any(Conta.class))).thenAnswer(i -> i.getArgument(0));
        service = new ContaService(repository);
    }

    @Test
    void criaContaAtivaComSaldoZeroAgenciaPadraoENumeroNoFormatoEsperado() {
        Conta c = service.criarConta(TipoConta.POUPANCA);

        assertEquals("0001", c.getAgencia());
        assertEquals(0, c.getSaldo().compareTo(BigDecimal.ZERO));
        assertEquals(StatusConta.ATIVA, c.getStatus());
        assertEquals(TipoConta.POUPANCA, c.getTipoConta());
        assertTrue(c.getNumeroConta().matches("\\d{6}-\\d"), "formato 123456-7, veio " + c.getNumeroConta());
    }

    @Test
    void geraOutroNumeroQuandoOPrimeiroJaExiste() {
        when(repository.existsByNumeroConta(anyString())).thenReturn(true, false);

        service.criarConta(TipoConta.CORRENTE);

        verify(repository, times(2)).existsByNumeroConta(anyString());
    }

    @Test
    void buscarPorNumeroEncontraOuLancaErroDeNegocio() {
        Conta c = Conta.builder().numeroConta("123456-7").build();
        when(repository.findByNumeroConta("123456-7")).thenReturn(Optional.of(c));
        when(repository.findByNumeroConta("000000-0")).thenReturn(Optional.empty());

        assertSame(c, service.buscarPorNumero("123456-7"));
        assertThrows(BusinessException.class, () -> service.buscarPorNumero("000000-0"));
    }
}
