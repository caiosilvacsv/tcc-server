package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.order.dto.OrderResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.user.User;
import br.edu.ifnmg.pagtesouro.domain.user.UserRoles;
import br.edu.ifnmg.pagtesouro.services.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Controlador de Pedidos (OrderController)")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private User user;
    private UUID orderId;
    private OrderResponseDTO orderResponseDTO;

    @BeforeEach
    void setUp() {
        user = new User(
            UUID.randomUUID(),
            "aluno@ifnmg.edu.br",
            "Aluno",
            "IFNMG",
            "hash123",
            "11144477735",
            UserRoles.USER,
            Instant.now(),
            Instant.now()
        );

        orderId = UUID.randomUUID();

        orderResponseDTO = new OrderResponseDTO(
            orderId,
            new BigDecimal("3.50"),
            OrderStatus.CANCELLED,
            Instant.now(),
            List.of(),
            "Aluno IFNMG",
            "11144477735",
            br.edu.ifnmg.pagtesouro.domain.order.BuyerType.STUDENT
        );
    }

    @Test
    @DisplayName("Deve cancelar pedido com sucesso e retornar 200 OK sem payload de corpo")
    void cancelOrderSuccess() {
        when(orderService.cancelOrder(orderId, user)).thenReturn(orderResponseDTO);

        ResponseEntity<OrderResponseDTO> response = orderController.cancelOrder(orderId, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(orderId, response.getBody().id());
        assertEquals(OrderStatus.CANCELLED, response.getBody().status());
        verify(orderService, times(1)).cancelOrder(orderId, user);
    }
}
