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
 * Objeto de Transferência de Dados (DTO) contendo a resposta detalhada e estruturada
 * de um pedido criado ou consultado no ecossistema do IFNMG.
 * <p>
 * **Conceito no TCC:**
 * Modela a projeção unificada do cabeçalho da transação do estudante, servindo como a resposta REST principal
 * que expõe a agregação financeira e de estados dos itens do carrinho.
 * </p>
 *
 * @param id Identificador único global (UUID) do pedido no banco de dados local
 * @param totalAmount Valor monetário total consolidado do pedido (soma do valor total de todos os itens)
 * @param status Estado consolidado atual do ciclo de vida do pedido (ex: CREATED, PENDING_PAYMENT, COMPLETED)
 * @param createdAt Carimbo de data/hora (Instant) da criação original do pedido
 * @param items Lista detalhada contendo a projeção de cada item individual de cobrança pertencente a este pedido
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
            order.getOrderItems().stream()
                .map(OrderItemResponseDTO::new)
                .collect(Collectors.toList())
        );
    }

    /**
     * Objeto de Transferência de Dados interno (record) para representar a projeção detalhada de cada item de cobrança do pedido.
     * <p>
     * Fornece um mapeamento granular que conecta a quantidade comprada, o preço unitário e o status do pagamento individualizado.
     * </p>
     *
     * @param id Identificador único global (UUID) do item de pedido no banco de dados
     * @param productId Identificador único do produto/serviço acadêmico correspondente
     * @param productTitle Título legível do serviço ou taxa (ex: "Tíquete de Refeição RU")
     * @param quantity Quantidade de unidades adquiridas deste item de cobrança
     * @param totalAmount Valor total acumulado para este item (preço unitário multiplicado pela quantidade)
     * @param status Status individualizado de quitação deste item de cobrança (ex: PENDING, PAID)
     */
    public record OrderItemResponseDTO(
        UUID id,
        UUID productId,
        String productTitle,
        Integer quantity,
        BigDecimal totalAmount,
        String status
    ) {
        /**
         * Construtor de conversão direta que permite instanciar o DTO do item de resposta a partir de uma entidade {@link OrderItem}.
         *
         * @param item A entidade JPA contendo os dados do item individual original do banco de dados
         */
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
