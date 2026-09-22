package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductCategoryResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductResponseDTO;
import br.edu.ifnmg.pagtesouro.services.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Controlador de Produtos (ProductController)")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Authentication auth;
    private SecurityContext securityContext;
    private ProductResponseDTO activeProduct;
    private ProductResponseDTO inactiveProduct;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        securityContext = mock(SecurityContext.class);

        activeProduct = new ProductResponseDTO(
            UUID.randomUUID(),
            "Almoço",
            "Tíquete de almoço",
            "almoco.png",
            new BigDecimal("3.50"),
            true,
            "028031",
            ProductCategory.TICKET
        );

        inactiveProduct = new ProductResponseDTO(
            UUID.randomUUID(),
            "Janta - Antigo",
            "Tíquete desativado",
            "janta.png",
            new BigDecimal("2.00"),
            false,
            "028032",
            ProductCategory.TICKET
        );
    }

    @SuppressWarnings({ "rawtypes" })
    @Test
    @DisplayName("Usuário comum requisitando getAll com activeOnly=false deve ver apenas ativos")
    void getAllForNormalUserFailsToSeeInactive() {
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(auth).getAuthorities();

        when(productService.findAllActive()).thenReturn(List.of(activeProduct));

        ResponseEntity<List<ProductResponseDTO>> response = productController.getAll(false);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Almoço", response.getBody().get(0).title());
        assertTrue(response.getBody().get(0).active());

        verify(productService, times(1)).findAllActive();
        verify(productService, never()).findAll();
        
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings({ "rawtypes" })
    @Test
    @DisplayName("Administrador requisitando getAll com activeOnly=false deve ver todos os produtos")
    void getAllForAdminSeesAll() {
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(auth).getAuthorities();

        when(productService.findAll()).thenReturn(List.of(activeProduct, inactiveProduct));

        ResponseEntity<List<ProductResponseDTO>> response = productController.getAll(false);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().get(0).active());
        assertFalse(response.getBody().get(1).active());

        verify(productService, times(1)).findAll();
        verify(productService, never()).findAllActive();
        
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Administrador requisitando getAll com activeOnly=true deve ver apenas ativos")
    void getAllForAdminActiveOnly() {
        when(productService.findAllActive()).thenReturn(List.of(activeProduct));

        ResponseEntity<List<ProductResponseDTO>> response = productController.getAll(true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().get(0).active());

        verify(productService, times(1)).findAllActive();
        verify(productService, never()).findAll();
    }

    @Test
    @DisplayName("Usuário sem login (auth nulo) requisitando getAll deve ver apenas ativos")
    void getAllForAnonymousUserSuccess() {
        when(productService.findAllActive()).thenReturn(List.of(activeProduct));

        ResponseEntity<List<ProductResponseDTO>> response = productController.getAll(true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Almoço", response.getBody().get(0).title());

        verify(productService, times(1)).findAllActive();
        verify(productService, never()).findAll();
    }

    @Test
    @DisplayName("Deve buscar produto por ID com sucesso")
    void getByIdSuccess() {
        UUID id = activeProduct.id();
        when(productService.findById(id)).thenReturn(activeProduct);

        ResponseEntity<ProductResponseDTO> response = productController.getById(id);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(activeProduct.title(), response.getBody().title());
        verify(productService, times(1)).findById(id);
    }

    @Test
    @DisplayName("Deve cadastrar produto com sucesso")
    void createSuccess() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        try {
            ProductRequestDTO request = new ProductRequestDTO(
                "Almoço",
                "Tíquete de almoço",
                "almoco.png",
                new BigDecimal("3.50"),
                true,
                "028031",
                ProductCategory.TICKET
            );

            when(productService.create(request)).thenReturn(activeProduct);

            ResponseEntity<ProductResponseDTO> response = productController.create(request);

            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals(activeProduct.title(), response.getBody().title());
            verify(productService, times(1)).create(request);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("Deve retornar categorias de produtos com sucesso")
    void getCategoriesSuccess() {
        var categoryDto = new ProductCategoryResponseDTO(ProductCategory.TICKET);
        when(productService.findCategories(true)).thenReturn(List.of(categoryDto));

        ResponseEntity<List<ProductCategoryResponseDTO>> response = productController.getCategories(true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("TICKET", response.getBody().get(0).key());
        verify(productService, times(1)).findCategories(true);
    }
}
