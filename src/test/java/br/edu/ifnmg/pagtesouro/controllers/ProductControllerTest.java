package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.dto.ProductResponseDTO;
import br.edu.ifnmg.pagtesouro.services.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.edu.ifnmg.pagtesouro.infra.security.SecurityConfiguration;
import br.edu.ifnmg.pagtesouro.infra.security.SecurityFilter;
import br.edu.ifnmg.pagtesouro.infra.security.TokenService;
import br.edu.ifnmg.pagtesouro.repository.UserRepository;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfiguration.class, SecurityFilter.class})
@DisplayName("Testes de Controle de Rotas do Produto (ProductController)")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenService tokenService;

    private ProductResponseDTO activeProduct;
    private ProductResponseDTO inactiveProduct;

    @BeforeEach
    void setUp() {
        activeProduct = new ProductResponseDTO(
            UUID.randomUUID(),
            "Almoço - RU",
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

    @Test
    @WithMockUser(username = "aluno@ifnmg.edu.br", roles = "USER")
    @DisplayName("Usuário comum requisitando com activeOnly=false deve ter filtro forçado e ver apenas ativos")
    void getAllForNormalUserFailsToSeeInactive() throws Exception {
        when(productService.findAllActive()).thenReturn(List.of(activeProduct));

        mockMvc.perform(get("/product")
                .param("activeOnly", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Almoço - RU"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(productService, times(1)).findAllActive();
        verify(productService, never()).findAll();
    }

    @Test
    @WithMockUser(username = "admin@ifnmg.edu.br", roles = "ADMIN")
    @DisplayName("Administrador requisitando com activeOnly=false deve ver todos os produtos (inclusive inativos)")
    void getAllForAdminSeesAll() throws Exception {
        when(productService.findAll()).thenReturn(List.of(activeProduct, inactiveProduct));

        mockMvc.perform(get("/product")
                .param("activeOnly", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(productService, times(1)).findAll();
        verify(productService, never()).findAllActive();
    }

    @Test
    @WithMockUser(username = "admin@ifnmg.edu.br", roles = "ADMIN")
    @DisplayName("Administrador requisitando com activeOnly=true deve ver apenas os produtos ativos")
    void getAllForAdminActiveOnly() throws Exception {
        when(productService.findAllActive()).thenReturn(List.of(activeProduct));

        mockMvc.perform(get("/product")
                .param("activeOnly", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(productService, times(1)).findAllActive();
        verify(productService, never()).findAll();
    }

    @Test
    @DisplayName("Usuário sem login (anônimo) requisitando GET /product deve conseguir ver apenas produtos ativos")
    void getAllForAnonymousUserSuccess() throws Exception {
        when(productService.findAllActive()).thenReturn(List.of(activeProduct));

        mockMvc.perform(get("/product")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Almoço - RU"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(productService, times(1)).findAllActive();
        verify(productService, never()).findAll();
    }
}
