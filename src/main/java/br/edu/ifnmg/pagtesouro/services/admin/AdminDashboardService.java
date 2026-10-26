package br.edu.ifnmg.pagtesouro.services.admin;

import br.edu.ifnmg.pagtesouro.domain.admin.dto.AdminDashboardResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.repository.OrderItemRepository;
import br.edu.ifnmg.pagtesouro.repository.OrderRepository;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serviço responsável por compilar as métricas analíticas e financeiras para o painel de administração.
 *
 * @author Caio da Silva Viana
 */
@Service
public class AdminDashboardService {

    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardService(
            PaymentRepository paymentRepository,
            OrderItemRepository orderItemRepository,
            OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Retorna o resumo consolidado de arrecadação e fluxo de tíquetes comprados vs trocados.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponseDTO getDashboardMetrics() {
        BigDecimal totalRevenue = paymentRepository.sumTotalConfirmedRevenue();
        long totalTicketsBought = orderItemRepository.countTotalBoughtTickets();
        long totalTicketsExchanged = orderItemRepository.countTotalExchangedTickets();
        long totalOrders = orderRepository.count();

        Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
        Map<String, String> ordersByStatusDescriptions = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status, orderRepository.countByStatus(status));
            ordersByStatusDescriptions.put(status.name(), status.getStatus());
        }

        return new AdminDashboardResponseDTO(
            totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
            totalTicketsBought,
            totalTicketsExchanged,
            totalOrders,
            ordersByStatus,
            ordersByStatusDescriptions
        );
    }
}
