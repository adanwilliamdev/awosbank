package com.awosbank.api.service;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final SecureRandom random = new SecureRandom();

    public Conta criarConta(TipoConta tipo) {
        String numero = gerarNumeroConta();
        while (contaRepository.existsByNumeroConta(numero)) {
            numero = gerarNumeroConta();
        }
        Conta conta = Conta.builder()
                .numeroConta(numero)
                .agencia("0001")
                .saldo(BigDecimal.ZERO)
                .tipoConta(tipo)
                .status(StatusConta.ATIVA)
                .dataAbertura(LocalDateTime.now())
                .build();
        return contaRepository.save(conta);
    }

    private String gerarNumeroConta() {
        int base = 100000 + random.nextInt(900000);
        return String.valueOf(base) + "-" + calcularDigito(base);
    }

    private int calcularDigito(int numero) {
        int soma = 0, peso = 2;
        while (numero > 0) {
            soma += (numero % 10) * peso;
            numero /= 10;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        int dv = 11 - resto;
        return (dv >= 10) ? 0 : dv;
    }

    public Conta buscarPorNumero(String numero) {
        return contaRepository.findByNumeroConta(numero)
                .orElseThrow(() -> new BusinessException("Conta destino não encontrada: " + numero));
    }
}