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

    @Test
    @DisplayName("Deve mascarar CPF no formato 123.***.***-01 para CPFs válidos com ou sem pontuação")
    void mask_ValidCpfs() {
        assertEquals("123.***.***-01", CpfValidator.mask("123.456.789-01"));
        assertEquals("123.***.***-01", CpfValidator.mask("12345678901"));
        assertEquals("111.***.***-35", CpfValidator.mask("111.444.777-35"));
        assertEquals("111.***.***-35", CpfValidator.mask("11144477735"));
        assertEquals("529.***.***-25", CpfValidator.mask("529.982.247-25"));
    }

    @Test
    @DisplayName("Deve retornar null ao mascarar CPF nulo ou em branco")
    void mask_NullOrBlank() {
        assertNull(CpfValidator.mask(null));
        assertNull(CpfValidator.mask(""));
        assertNull(CpfValidator.mask("   "));
    }

    @Test
    @DisplayName("Deve retornar máscara segura quando CPF não contiver exatamente 11 dígitos (ex: CNPJ ou inválido)")
    void mask_Non11Digits() {
        assertEquals("***.***.***-**", CpfValidator.mask("12345"));
        assertEquals("***.***.***-**", CpfValidator.mask("12.345.678/0001-90"));
        assertEquals("***.***.***-**", CpfValidator.mask("abc-def"));
    }
}
