package com.awosbank.api.service;

import com.awosbank.api.domain.enums.TipoChavePix;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.PixKeyRepository;
import com.awosbank.api.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Valida o formato de cada tipo de chave — inclusive payloads de XSS. */
class PixKeyServiceTest {

    private PixKeyService service;
    private Usuario titular;

    @BeforeEach
    void setUp() {
        service = new PixKeyService(Mockito.mock(PixKeyRepository.class));
        titular = Usuario.builder().cpfCnpj("11144477735").build();
    }

    @Test
    void cpfDoTitularEhNormalizadoParaDigitos() {
        assertEquals("11144477735", service.normalizarEValidar(titular, TipoChavePix.CPF, "111.444.777-35"));
    }

    @Test
    void cpfDeOutraPessoaEhRejeitado() {
        // CPF válido, mas não é o do titular
        assertThrows(BusinessException.class,
                () -> service.normalizarEValidar(titular, TipoChavePix.CPF, "529.982.247-25"));
    }

    @Test
    void cpfInvalidoEhRejeitado() {
        assertThrows(BusinessException.class,
                () -> service.normalizarEValidar(titular, TipoChavePix.CPF, "11144477736"));
    }

    @Test
    void emailEhNormalizadoParaMinusculas() {
        assertEquals("joao@exemplo.com",
                service.normalizarEValidar(titular, TipoChavePix.EMAIL, "  Joao@Exemplo.COM "));
    }

    @Test
    void payloadXssNaoPassaEmNenhumTipo() {
        String xss = "<img src=x onerror=alert(1)>";
        for (TipoChavePix tipo : TipoChavePix.values()) {
            assertThrows(BusinessException.class,
                    () -> service.normalizarEValidar(titular, tipo, xss),
                    "tipo " + tipo + " aceitou payload XSS");
        }
    }

    @Test
    void telefoneAceitaMascaraEDevolveSoDigitos() {
        assertEquals("11987654321", service.normalizarEValidar(titular, TipoChavePix.TELEFONE, "(11) 98765-4321"));
        assertThrows(BusinessException.class,
                () -> service.normalizarEValidar(titular, TipoChavePix.TELEFONE, "12345"));
    }

    @Test
    void aleatoriaEmBrancoGeraUuid() {
        String chave = service.normalizarEValidar(titular, TipoChavePix.ALEATORIA, "");
        assertDoesNotThrow(() -> UUID.fromString(chave));
    }

    @Test
    void aleatoriaInformadaPrecisaSerUuid() {
        assertThrows(BusinessException.class,
                () -> service.normalizarEValidar(titular, TipoChavePix.ALEATORIA, "minha-chave"));
    }
}
