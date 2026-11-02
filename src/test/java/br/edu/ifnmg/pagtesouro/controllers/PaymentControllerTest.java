package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.DirectCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.OrderCheckoutRequestDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Controlador de Pagamentos (PaymentController)")
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private User user;
    private CheckoutResponseDTO checkoutResponseDTO;

    @BeforeEach
    void setUp() {
        user = new User(
            UUID.randomUUID(),
            "aluno@ifnmg.edu.br",
            "Aluno",
            "Silva",
            "hash123",
            "11144477735",
            UserRoles.USER,
            Instant.now(),
            Instant.now()
        );

        checkoutResponseDTO = new CheckoutResponseDTO(
            UUID.randomUUID(),
            "PT123456",
            new BigDecimal("5.00"),
            "CREATED",
            "https://pagtesouro.tesouro.gov.br/checkout/123"
        );
    }

    @Test
    @DisplayName("Canonicalização: Mapeamento de rotas em PaymentController não deve conter aliases obsoletos")
    void verifyCanonicalRouteMappings() throws Exception {
        Method directMethod = PaymentController.class.getMethod("createPaymentAnonymous", DirectCheckoutRequestDTO.class);
        PostMapping directAnnotation = directMethod.getAnnotation(PostMapping.class);
        assertNotNull(directAnnotation);
        List<String> directPaths = Arrays.asList(directAnnotation.value().length > 0 ? directAnnotation.value() : directAnnotation.path());
        assertTrue(directPaths.contains("/checkout/direct"), "Deve mapear /checkout/direct");
        assertFalse(directPaths.contains("/anonymous"), "Não deve conter alias obsoleto /anonymous");
        assertEquals(1, directPaths.size(), "Deve expor apenas uma rota canônica");

        Method checkoutMethod = PaymentController.class.getMethod("createPaymentCheckout", UUID.class, OrderCheckoutRequestDTO.class, User.class);
        PostMapping checkoutAnnotation = checkoutMethod.getAnnotation(PostMapping.class);
        assertNotNull(checkoutAnnotation);
        List<String> checkoutPaths = Arrays.asList(checkoutAnnotation.value().length > 0 ? checkoutAnnotation.value() : checkoutAnnotation.path());
        assertTrue(checkoutPaths.contains("/checkout/{orderId}"), "Deve mapear /checkout/{orderId}");
        assertFalse(checkoutPaths.contains("/checkout/order/{orderId}"), "Não deve conter alias obsoleto /checkout/order/{orderId}");
        assertEquals(1, checkoutPaths.size(), "Deve expor apenas uma rota canônica");
    }

    @Test
    @DisplayName("Deve efetuar checkout direto com sucesso e retornar 201 Created")
    void createPaymentDirectSuccess() {
        DirectCheckoutRequestDTO request = new DirectCheckoutRequestDTO(
            UUID.randomUUID(),
            2,
            "11144477735",
            "Aluno IFNMG",
            false
        );

        when(paymentService.checkoutDirect(request)).thenReturn(checkoutResponseDTO);

        ResponseEntity<CheckoutResponseDTO> response = paymentController.createPaymentAnonymous(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(checkoutResponseDTO.paymentId(), response.getBody().paymentId());
        assertEquals(checkoutResponseDTO.nextUrl(), response.getBody().nextUrl());
        verify(paymentService, times(1)).checkoutDirect(request);
    }

    @Test
    @DisplayName("Deve efetuar checkout de pedido com sucesso e retornar 201 Created")
    void createPaymentCheckoutSuccess() {
        UUID orderId = UUID.randomUUID();
        OrderCheckoutRequestDTO request = new OrderCheckoutRequestDTO("11144477735", "Aluno IFNMG", false);
        List<CheckoutResponseDTO> list = List.of(checkoutResponseDTO);

        when(paymentService.checkoutOrder(eq(orderId), eq(request), eq(user))).thenReturn(list);

        ResponseEntity<List<CheckoutResponseDTO>> response = paymentController.createPaymentCheckout(orderId, request, user);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(checkoutResponseDTO.paymentId(), response.getBody().get(0).paymentId());
        verify(paymentService, times(1)).checkoutOrder(orderId, request, user);
    }

    @Test
    @DisplayName("Deve efetuar checkout de pedido com request nula usando fallback padrão")
    void createPaymentCheckoutWithNullRequest() {
        UUID orderId = UUID.randomUUID();
        List<CheckoutResponseDTO> list = List.of(checkoutResponseDTO);

        when(paymentService.checkoutOrder(eq(orderId), any(OrderCheckoutRequestDTO.class), eq(user))).thenReturn(list);

        ResponseEntity<List<CheckoutResponseDTO>> response = paymentController.createPaymentCheckout(orderId, null, user);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(paymentService, times(1)).checkoutOrder(eq(orderId), any(OrderCheckoutRequestDTO.class), eq(user));
    }
}
