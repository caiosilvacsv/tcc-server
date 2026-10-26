package br.edu.ifnmg.pagtesouro.domain.admin.dto;

import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DTO para retorno das métricas consolidadas da arrecadação e tíquetes do campus para o painel de ADMIN.
 *
 * @author Caio da Silva Viana
 */
public record AdminDashboardResponseDTO(
    BigDecimal totalRevenue,
    long totalTicketsBought,
    long totalTicketsExchanged,
    long totalOrders,
    Map<OrderStatus, Long> ordersByStatus,
    Map<String, String> ordersByStatusDescriptions
) {
    public AdminDashboardResponseDTO(
        BigDecimal totalRevenue,
        long totalTicketsBought,
        long totalTicketsExchanged,
        long totalOrders,
        Map<OrderStatus, Long> ordersByStatus
    ) {
        this(
            totalRevenue,
            totalTicketsBought,
            totalTicketsExchanged,
            totalOrders,
            ordersByStatus,
            buildStatusDescriptions(ordersByStatus)
        );
    }

    private static Map<String, String> buildStatusDescriptions(Map<OrderStatus, Long> ordersByStatus) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        if (ordersByStatus != null) {
            for (OrderStatus status : ordersByStatus.keySet()) {
                descriptions.put(status.name(), status.getStatus());
            }
        }
        return descriptions;
    }
}
