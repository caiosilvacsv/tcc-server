package br.edu.ifnmg.pagtesouro.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da Entidade de Produto (Product)")
class ProductTest {

    @Test
    @DisplayName("Deve validar a modelagem e atribuições de campos do Produto")
    void productProperties() {
        Product product = new Product();
        UUID id = UUID.randomUUID();
        
        product.setId(id);
        product.setTitle("Almoço - Estudante");
        product.setDescription("Tíquete de alimentação para almoço estudantil");
        product.setImage("almoco.png");
        product.setPrice(new BigDecimal("3.50"));
        product.setActive(true);
        product.setCodeService("028031");
        product.setCategory(ProductCategory.TICKET);
        
        assertEquals(id, product.getId());
        assertEquals("Almoço - Estudante", product.getTitle());
        assertEquals("Tíquete de alimentação para almoço estudantil", product.getDescription());
        assertEquals("almoco.png", product.getImage());
        assertEquals(new BigDecimal("3.50"), product.getPrice());
        assertTrue(product.isActive());
        assertEquals("028031", product.getCodeService());
        assertEquals(ProductCategory.TICKET, product.getCategory());
    }
}
