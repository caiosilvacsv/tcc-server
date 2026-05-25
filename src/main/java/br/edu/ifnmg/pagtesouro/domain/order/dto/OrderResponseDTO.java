package br.edu.ifnmg.pagtesouro.domain.order.dto;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO contendo a resposta detalhada e estruturada de um pedido criado ou consultado.
 *
 * @author Caio da Silva Viana
 */
public record OrderResponseDTO(
    UUID id,
    BigDecimal totalAmount,
    OrderStatus status,
    Instant createdAt,
    List<OrderItemResponseDTO> items
) {
    public OrderResponseDTO(Order order) {
        this(
            order.getId(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreateAt(),
            order.getOrderItems().stream()
                .map(OrderItemResponseDTO::new)
                .collect(Collectors.toList())
        );
    }

    /**
     * DTO interno para representar cada item do pedido.
     */
    public record OrderItemResponseDTO(
        UUID id,
        UUID productId,
        String productTitle,
        Integer quantity,
        BigDecimal totalAmount,
        String status
    ) {
        public OrderItemResponseDTO(OrderItem item) {
            this(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getTitle(),
                item.getQuantity(),
                item.getTotalAmount(),
                item.getStatus().name()
            );
        }
    }
}
