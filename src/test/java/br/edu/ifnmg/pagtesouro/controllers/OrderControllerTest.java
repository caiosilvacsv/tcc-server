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

    @Test
    @DisplayName("getItemById: Deve repassar o usuário autenticado para o OrderService e retornar 200 OK")
    void getItemByIdSuccess() {
        UUID itemId = UUID.randomUUID();
        br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO ticketDto =
            new br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO(
                itemId,
                orderId,
                "Refeição",
                1,
                new BigDecimal("2.50"),
                "Aluno",
                "11144477735",
                br.edu.ifnmg.pagtesouro.domain.order.BuyerType.STUDENT,
                Instant.now()
            );

        when(orderService.getItemById(itemId, user)).thenReturn(ticketDto);

        ResponseEntity<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO> response =
            orderController.getItemById(itemId, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(itemId, response.getBody().itemId());
        verify(orderService, times(1)).getItemById(itemId, user);
    }

    @Test
    @DisplayName("getAvailableTickets: Próprio CPF com ou sem pontuação deve ser autorizado com 200 OK")
    void getAvailableTicketsSelfCpfAllowed() {
        List<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO> tickets = List.of();
        when(orderService.getTicketsAvailableByCpf("111.444.777-35")).thenReturn(tickets);

        ResponseEntity<List<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO>> response =
            orderController.getAvailableTickets("111.444.777-35", user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderService, times(1)).getTicketsAvailableByCpf("111.444.777-35");
    }

    @Test
    @DisplayName("getAvailableTickets: Consulta sem CPF informado utiliza o CPF do próprio usuário")
    void getAvailableTicketsNoCpfParamUsesUserCpf() {
        List<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO> tickets = List.of();
        when(orderService.getTicketsAvailableByCpf(user.getCpf())).thenReturn(tickets);

        ResponseEntity<List<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO>> response =
            orderController.getAvailableTickets(null, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderService, times(1)).getTicketsAvailableByCpf(user.getCpf());
    }

    @Test
    @DisplayName("getAvailableTickets: CPF de terceiro por usuário comum (USER) deve lançar SecurityException (403)")
    void getAvailableTicketsOtherCpfForbiddenForNonAdmin() {
        String otherCpf = "52998224725";

        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> orderController.getAvailableTickets(otherCpf, user)
        );

        assertTrue(exception.getMessage().contains("apenas administradores"));
        verify(orderService, never()).getTicketsAvailableByCpf(any());
    }

    @Test
    @DisplayName("getAvailableTickets: CPF de terceiro por usuário ADMIN deve ser autorizado com 200 OK")
    void getAvailableTicketsOtherCpfAllowedForAdmin() {
        User admin = new User(
            UUID.randomUUID(),
            "admin@ifnmg.edu.br",
            "Admin",
            "IFNMG",
            "hash123",
            "99988877766",
            UserRoles.ADMIN,
            Instant.now(),
            Instant.now()
        );

        String otherCpf = "52998224725";
        List<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO> tickets = List.of();
        when(orderService.getTicketsAvailableByCpf(otherCpf)).thenReturn(tickets);

        ResponseEntity<List<br.edu.ifnmg.pagtesouro.domain.order.dto.TicketReadyResponseDTO>> response =
            orderController.getAvailableTickets(otherCpf, admin);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(orderService, times(1)).getTicketsAvailableByCpf(otherCpf);
    }

    @Test
    @DisplayName("getGuestTickets: Rota pública de visitantes desativada deve lançar SecurityException")
    void getGuestTicketsRouteDeactivatedThrowsSecurityException() {
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> orderController.getGuestTickets("11144477735", "Aluno")
        );

        assertTrue(exception.getMessage().contains("desativada"));
        verifyNoInteractions(orderService);
    }
}
