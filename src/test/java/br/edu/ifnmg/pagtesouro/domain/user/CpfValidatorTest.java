package br.edu.ifnmg.pagtesouro.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Validador de CPF (CpfValidator)")
class CpfValidatorTest {

    @Test
    @DisplayName("Deve normalizar CPFs removendo caracteres não numéricos")
    void normalize() {
        assertEquals("12345678909", CpfValidator.normalize("123.456.789-09"));
        assertEquals("12345678909", CpfValidator.normalize("12345678909"));
        assertEquals("12345678909", CpfValidator.normalize("123-456 789/09"));
        assertEquals("", CpfValidator.normalize("abc-def"));
        assertNull(CpfValidator.normalize(null));
    }

    @Test
    @DisplayName("Deve retornar true para CPFs matematicamente válidos")
    void isValid_True() {
        // CPF gerado válido para testes
        assertTrue(CpfValidator.isValid("111.444.777-35"));
        assertTrue(CpfValidator.isValid("11144477735"));
        assertTrue(CpfValidator.isValid("529.982.247-25"));
        assertTrue(CpfValidator.isValid("52998224725"));
    }


    @Test
    @DisplayName("Deve retornar false para CPFs matematicamente inválidos")
    void isValid_False() {
        // CPFs inválidos
        assertFalse(CpfValidator.isValid("111.444.777-36")); // Dígito incorreto
        assertFalse(CpfValidator.isValid("12345678900"));
        assertFalse(CpfValidator.isValid(""));
        assertFalse(CpfValidator.isValid(null));
        assertFalse(CpfValidator.isValid("12345")); // Menor que 11 dígitos
        assertFalse(CpfValidator.isValid("123456789012")); // Maior que 11 dígitos
    }

    @Test
    @DisplayName("Deve rejeitar CPFs conhecidos com dígitos todos repetidos")
    void isValid_RepeatedDigits() {
        assertFalse(CpfValidator.isValid("000.000.000-00"));
        assertFalse(CpfValidator.isValid("111.111.111-11"));
        assertFalse(CpfValidator.isValid("99999999999"));
    }
}
