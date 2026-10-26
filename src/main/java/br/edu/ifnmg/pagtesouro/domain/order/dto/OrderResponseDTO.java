package br.edu.ifnmg.pagtesouro.domain.order.dto;

import br.edu.ifnmg.pagtesouro.domain.order.BuyerType;
import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Objeto de Transferência de Dados (DTO) contendo a resposta detalhada e estruturada
 * de um pedido criado ou consultado no ecossistema do IFNMG.
 * <p>
 * <b>Conceito no TCC:</b>
 * Modela a projeção unificada do cabeçalho da transação do estudante ou visitante,
 * servindo como a resposta REST principal que expõe a agregação financeira, de estados dos itens
 * e flags de controle de interface (Server-Driven UI: {@code canPay}, {@code canCancel}, {@code statusDescription}).
 * </p>
 *
 * @author Caio da Silva Viana
 */
public record OrderResponseDTO(
    UUID id,
    BigDecimal totalAmount,
    OrderStatus status,
    Instant createdAt,
    List<OrderItemResponseDTO> items,
    String buyerName,
    String buyerCpf,
    BuyerType buyerType,
    String statusDescription,
    boolean canPay,
    boolean canCancel
) {
    /**
     * Construtor de compatibilidade para chamadas com 5 parâmetros.
     */
    public OrderResponseDTO(
        UUID id,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponseDTO> items
    ) {
        this(
            id,
            totalAmount,
            status,
            createdAt,
            items,
            null,
            null,
            null,
            resolveStatusDescription(status),
            computeCanPay(status, items),
            computeCanCancel(status, items)
        );
    }

    /**
     * Construtor de compatibilidade para chamadas com 8 parâmetros.
     */
    public OrderResponseDTO(
        UUID id,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemResponseDTO> items,
        String buyerName,
        String buyerCpf,
        BuyerType buyerType
    ) {
        this(
            id,
            totalAmount,
            status,
            createdAt,
            items,
            buyerName,
            buyerCpf,
            buyerType,
            resolveStatusDescription(status),
            computeCanPay(status, items),
            computeCanCancel(status, items)
        );
    }

    /**
     * Construtor de conversão direta que permite instanciar o DTO de resposta a partir de uma entidade {@link Order}.
     *
     * @param order A entidade JPA de Pedido contendo os dados originais do banco de dados
     */
    public OrderResponseDTO(Order order) {
        this(
            order.getId(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreateAt(),
            order.getOrderItems() != null ? order.getOrderItems().stream()
                .map(OrderItemResponseDTO::new)
                .collect(Collectors.toList()) : List.of(),
            order.getBuyerName(),
            order.getBuyerCpf(),
            order.getBuyerType()
        );
    }

    private static String resolveStatusDescription(OrderStatus status) {
        if (status == null) return "Desconhecido";
        return switch (status) {
            case CREATED -> "Criado (Aguardando Guia)";
            case PENDING_PAYMENT -> "Aguardando Pagamento";
            case PARTIALLY_PAID -> "Pago Parcialmente";
            case COMPLETED -> "Concluído / Quitado";
            case CANCELLED -> "Cancelado";
        };
    }

    private static boolean computeCanPay(OrderStatus status, List<OrderItemResponseDTO> items) {
        if (status == null || status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return false;
        }
        return items != null && items.stream().anyMatch(i -> "PENDING".equalsIgnoreCase(i.status()));
    }

    private static boolean computeCanCancel(OrderStatus status, List<OrderItemResponseDTO> items) {
        if (status == null || status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return false;
        }
        if (items == null || items.isEmpty()) {
            return true;
        }
        return items.stream().noneMatch(i -> "PAID".equalsIgnoreCase(i.status()) || "EXCHANGED".equalsIgnoreCase(i.status()));
    }

    /**
     * Objeto de Transferência de Dados interno (record) para representar a projeção detalhada de cada item de cobrança do pedido.
     */
    public record OrderItemResponseDTO(
        UUID id,
        UUID productId,
        String productTitle,
        Integer quantity,
        BigDecimal totalAmount,
        String status,
        Instant paidAt,
        Instant exchangedAt,
        String statusDescription,
        String productColor,
        String categoryDisplayName
    ) {
        /**
         * Construtor de compatibilidade para 6 parâmetros.
         */
        public OrderItemResponseDTO(
            UUID id,
            UUID productId,
            String productTitle,
            Integer quantity,
            BigDecimal totalAmount,
            String status
        ) {
            this(
                id,
                productId,
                productTitle,
                quantity,
                totalAmount,
                status,
                null,
                null,
                resolveItemStatusDescription(status),
                null,
                null
            );
        }

        /**
         * Construtor de compatibilidade para 8 parâmetros.
         */
        public OrderItemResponseDTO(
            UUID id,
            UUID productId,
            String productTitle,
            Integer quantity,
            BigDecimal totalAmount,
            String status,
            Instant paidAt,
            Instant exchangedAt
        ) {
            this(
                id,
                productId,
                productTitle,
                quantity,
                totalAmount,
                status,
                paidAt,
                exchangedAt,
                resolveItemStatusDescription(status),
                null,
                null
            );
        }

        /**
         * Construtor de conversão direta que permite instanciar o DTO do item de resposta a partir de uma entidade {@link OrderItem}.
         *
         * @param item A entidade JPA contendo os dados do item individual original do banco de dados
         */
        public OrderItemResponseDTO(OrderItem item) {
            this(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getTitle() : null,
                item.getQuantity(),
                item.getTotalAmount(),
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getPaidAt(),
                item.getExchangedAt(),
                resolveItemStatusDescription(item.getStatus() != null ? item.getStatus().name() : null),
                item.getProduct() != null ? item.getProduct().getColor() : null,
                (item.getProduct() != null && item.getProduct().getCategory() != null)
                    ? item.getProduct().getCategory().getDisplayName() : null
            );
        }

        private static String resolveItemStatusDescription(String status) {
            if (status == null) return "Pendente";
            return switch (status.toUpperCase()) {
                case "PENDING" -> "Aguardando Pagamento";
                case "PAID" -> "Pago (Disponível)";
                case "EXCHANGED" -> "Retirado no Guichê";
                case "CANCELLED" -> "Cancelado";
                default -> status;
            };
        }
    }
}
