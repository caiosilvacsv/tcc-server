package br.edu.ifnmg.pagtesouro.domain.order.dto;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.order.OrderStatus;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;

import br.edu.ifnmg.pagtesouro.domain.order.BuyerType;

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
 * servindo como a resposta REST principal que expõe a agregação financeira e de estados dos itens.
 * </p>
 *
 * @param id Identificador único global (UUID) do pedido no banco de dados local
 * @param totalAmount Valor monetário total consolidado do pedido (soma do valor total de todos os itens)
 * @param status Estado consolidado atual do ciclo de vida do pedido (ex: CREATED, PENDING_PAYMENT, COMPLETED)
 * @param createdAt Carimbo de data/hora (Instant) da criação original do pedido
 * @param items Lista detalhada contendo a projeção de cada item individual de cobrança pertencente a este pedido
 * @param buyerName Nome completo do comprador (aluno ou convidado)
 * @param buyerCpf CPF do comprador
 * @param buyerType Enquadramento do comprador ({@link BuyerType#STUDENT} ou {@link BuyerType#GUEST})
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
    BuyerType buyerType
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
        this(id, totalAmount, status, createdAt, items, null, null, null);
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

    /**
     * Objeto de Transferência de Dados interno (record) para representar a projeção detalhada de cada item de cobrança do pedido.
     * <p>
     * Fornece um mapeamento granular que conecta a quantidade comprada, o preço unitário e o status do pagamento individualizado.
     * </p>
     *
     * @param id Identificador único global (UUID) do item de pedido no banco de dados
     * @param productId Identificador único do produto/serviço acadêmico correspondente
     * @param productTitle Título legível do serviço ou taxa (ex: "Tíquete de Refeição")
     * @param quantity Quantidade de unidades adquiridas deste item de cobrança
     * @param totalAmount Valor total acumulado para este item (preço unitário multiplicado pela quantidade)
     * @param status Status individualizado de quitação deste item de cobrança (ex: PENDING, PAID, EXCHANGED)
     * @param paidAt Data e hora de liquidação bancária do item
     * @param exchangedAt Data e hora de baixa/troca física do tíquete no balcão
     */
    public record OrderItemResponseDTO(
        UUID id,
        UUID productId,
        String productTitle,
        Integer quantity,
        BigDecimal totalAmount,
        String status,
        Instant paidAt,
        Instant exchangedAt
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
            this(id, productId, productTitle, quantity, totalAmount, status, null, null);
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
                item.getExchangedAt()
            );
        }
    }
}
