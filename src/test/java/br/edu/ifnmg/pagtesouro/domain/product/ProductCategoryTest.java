package br.edu.ifnmg.pagtesouro.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da Categoria de Produto (ProductCategory)")
class ProductCategoryTest {

    @Test
    @DisplayName("Deve converter string correspondente para o Enum ProductCategory de forma segura")
    void fromValue_Success() {
        assertEquals(ProductCategory.TICKET, ProductCategory.fromValue("ticket"));
        assertEquals(ProductCategory.FINE, ProductCategory.fromValue("fine"));
        assertEquals(ProductCategory.TICKET, ProductCategory.fromValue("  TICKET  "));
        assertEquals(ProductCategory.FINE, ProductCategory.fromValue("Fine"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valores nulos ou inválidos")
    void fromValue_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> ProductCategory.fromValue(null));
        assertThrows(IllegalArgumentException.class, () -> ProductCategory.fromValue("invalido"));
        assertThrows(IllegalArgumentException.class, () -> ProductCategory.fromValue(""));
    }
}
