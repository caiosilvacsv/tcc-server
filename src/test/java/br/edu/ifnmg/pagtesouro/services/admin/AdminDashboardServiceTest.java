package br.edu.ifnmg.pagtesouro.services.admin;

import br.edu.ifnmg.pagtesouro.domain.admin.dto.AdminDashboardResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.repository.OrderItemRepository;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários do Painel Administrativo de Métricas (AdminDashboardService)")
class AdminDashboardServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("Deve calcular e consolidar as métricas financeiras e operacionais com precisão")
    void getDashboardMetricsSuccess() {
        when(paymentRepository.sumTotalConfirmedRevenue()).thenReturn(new BigDecimal("1250.50"));
        when(orderItemRepository.countTotalBoughtTickets()).thenReturn(500L);
        when(orderItemRepository.countTotalExchangedTickets()).thenReturn(350L);
        when(orderRepository.count()).thenReturn(55L);

        // Configuração de todos os status do enum para satisfazer Mockito strictness
        when(orderRepository.countByStatus(OrderStatus.CREATED)).thenReturn(0L);
        when(orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT)).thenReturn(10L);
        when(orderRepository.countByStatus(OrderStatus.PARTIALLY_PAID)).thenReturn(0L);
        when(orderRepository.countByStatus(OrderStatus.COMPLETED)).thenReturn(40L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(5L);

        AdminDashboardResponseDTO metrics = adminDashboardService.getDashboardMetrics();

        assertNotNull(metrics);
        assertEquals(new BigDecimal("1250.50"), metrics.totalRevenue());
        assertEquals(500L, metrics.totalTicketsBought());
        assertEquals(350L, metrics.totalTicketsExchanged());
        assertEquals(55L, metrics.totalOrders());
        assertEquals(40L, metrics.ordersByStatus().get(OrderStatus.COMPLETED));
        assertEquals(10L, metrics.ordersByStatus().get(OrderStatus.PENDING_PAYMENT));
        assertEquals(5L, metrics.ordersByStatus().get(OrderStatus.CANCELLED));
        assertEquals(0L, metrics.ordersByStatus().get(OrderStatus.CREATED));
        assertEquals(0L, metrics.ordersByStatus().get(OrderStatus.PARTIALLY_PAID));

        verify(paymentRepository, times(1)).sumTotalConfirmedRevenue();
        verify(orderItemRepository, times(1)).countTotalBoughtTickets();
        verify(orderItemRepository, times(1)).countTotalExchangedTickets();
        verify(orderRepository, times(1)).count();
    }
}
