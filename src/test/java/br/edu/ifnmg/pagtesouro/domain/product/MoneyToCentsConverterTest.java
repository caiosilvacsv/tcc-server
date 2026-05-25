package br.edu.ifnmg.pagtesouro.domain.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Conversor de Dinheiro para Centavos (MoneyToCentsConverter)")
class MoneyToCentsConverterTest {

    private MoneyToCentsConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MoneyToCentsConverter();
    }

    @Test
    @DisplayName("Deve converter BigDecimal para Centavos (Banco de Dados)")
    void convertToDatabaseColumn() {
        assertEquals(1050, converter.convertToDatabaseColumn(new BigDecimal("10.50")));
        assertEquals(100, converter.convertToDatabaseColumn(new BigDecimal("1.00")));
        assertEquals(99, converter.convertToDatabaseColumn(new BigDecimal("0.99")));
        assertEquals(0, converter.convertToDatabaseColumn(BigDecimal.ZERO));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    @DisplayName("Deve converter Centavos (Banco) para BigDecimal (Java)")
    void convertToEntityAttribute() {
        assertEquals(new BigDecimal("10.50"), converter.convertToEntityAttribute(1050));
        assertEquals(new BigDecimal("1.00"), converter.convertToEntityAttribute(100));
        assertEquals(new BigDecimal("0.99"), converter.convertToEntityAttribute(99));
        assertEquals(new BigDecimal("0.00"), converter.convertToEntityAttribute(0));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
