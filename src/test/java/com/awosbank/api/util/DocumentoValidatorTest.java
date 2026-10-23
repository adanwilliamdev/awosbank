package com.awosbank.api.util;

import com.awosbank.api.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoValidatorTest {

    @Test
    void aceitaCpfValidoComESemMascara() {
        assertTrue(DocumentoValidator.isValid("11144477735"));
        assertTrue(DocumentoValidator.isValid("111.444.777-35"));
        assertTrue(DocumentoValidator.isValid(TestData.cpfValido(42)));
    }

    @Test
    void rejeitaCpfInvalido() {
        assertFalse(DocumentoValidator.isValid("11144477736"));   // DV errado
        assertFalse(DocumentoValidator.isValid("11111111111"));   // todos iguais
        assertFalse(DocumentoValidator.isValid("123"));
        assertFalse(DocumentoValidator.isValid(null));
    }

    @Test
    void validaCnpj() {
        assertTrue(DocumentoValidator.isValid("11.222.333/0001-81"));
        assertFalse(DocumentoValidator.isValid("11.222.333/0001-82"));
    }
}
