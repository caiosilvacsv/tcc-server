package br.edu.ifnmg.pagtesouro.services.product;

import br.edu.ifnmg.pagtesouro.domain.product.Product;
import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductResponseDTO;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Serviço de Produtos (ProductService)")
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    private Product product;
    private ProductRequestDTO requestDTO;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setTitle("Almoço - RU");
        product.setDescription("Tíquete de alimentação para almoço");
        product.setImage("almoco.png");
        product.setPrice(new BigDecimal("3.50"));
        product.setActive(true);
        product.setCodeService("028031");
        product.setCategory(ProductCategory.TICKET);

        requestDTO = new ProductRequestDTO(
            "Almoço - RU",
            "Tíquete de alimentação para almoço",
            "almoco.png",
            new BigDecimal("3.50"),
            true,
            "028031",
            ProductCategory.TICKET
        );
    }

    @Test
    @DisplayName("Deve criar um produto com sucesso a partir de um DTO")
    void createProductSuccess() {
        when(repository.save(any(Product.class))).thenReturn(product);

        ProductResponseDTO response = service.create(requestDTO);

        assertNotNull(response);
        assertEquals(productId, response.id());
        assertEquals("Almoço - RU", response.title());
        assertEquals(new BigDecimal("3.50"), response.price());
        assertTrue(response.active());
        verify(repository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve atualizar um produto com sucesso")
    void updateProductSuccess() {
        when(repository.findById(productId)).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class))).thenReturn(product);

        ProductResponseDTO response = service.update(productId, requestDTO);

        assertNotNull(response);
        assertEquals("Almoço - RU", response.title());
        verify(repository, times(1)).findById(productId);
        verify(repository, times(1)).save(product);
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar atualizar um produto inexistente")
    void updateProductNotFound() {
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(FindException.class, () -> service.update(productId, requestDTO));
        verify(repository, times(1)).findById(productId);
        verify(repository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Deve buscar um produto pelo ID com sucesso")
    void findByIdSuccess() {
        when(repository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponseDTO response = service.findById(productId);

        assertNotNull(response);
        assertEquals(productId, response.id());
        verify(repository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Deve buscar produtos pelo título com sucesso, retornando uma lista")
    void findByTitleSuccess() {
        when(repository.findByTitle("Almoço - RU")).thenReturn(List.of(product));

        List<ProductResponseDTO> response = service.findByTitle("Almoço - RU");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Almoço - RU", response.get(0).title());
        verify(repository, times(1)).findByTitle("Almoço - RU");
    }

    @Test
    @DisplayName("Deve listar todos os produtos cadastrados")
    void findAllProducts() {
        when(repository.findAll()).thenReturn(List.of(product));

        List<ProductResponseDTO> response = service.findAll();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Almoço - RU", response.get(0).title());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve listar apenas produtos ativos")
    void findAllActiveProducts() {
        when(repository.findAllByActiveTrue()).thenReturn(List.of(product));

        List<ProductResponseDTO> response = service.findAllActive();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertTrue(response.get(0).active());
        verify(repository, times(1)).findAllByActiveTrue();
    }

    @Test
    @DisplayName("Deve desativar logicamente um produto (Soft Delete) com sucesso")
    void deleteProductSuccess() {
        when(repository.findById(productId)).thenReturn(Optional.of(product));
        when(repository.save(any(Product.class))).thenReturn(product);

        service.delete(productId);

        assertFalse(product.isActive());
        verify(repository, times(1)).findById(productId);
        verify(repository, times(1)).save(product);
    }
}
