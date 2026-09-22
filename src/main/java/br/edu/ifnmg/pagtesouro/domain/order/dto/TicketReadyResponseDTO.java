package br.edu.ifnmg.pagtesouro.domain.order.dto;

import br.edu.ifnmg.pagtesouro.domain.order.BuyerType;
import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) plano que projeta um tíquete ou serviço quitado
 * e disponível para baixa/retirada física no balcão de atendimento.
 * <p>
 * <b>Conceito no TCC:</b>
 * Unifica a representação de tíquetes pagos tanto para estudantes quanto para visitantes convidados,
 * eliminando estruturas aninhadas e permitindo que o atendente visualize de imediato os itens
 * em fila cronológica (FIFO) prontos para a confirmação de entrega.
 * </p>
 *
 * @param itemId Identificador único do item de pedido (utilizado na chamada de baixa/exchange)
 * @param orderId Identificador único do pedido de origem
 * @param productTitle Nome legível do produto ou refeição (ex: "Almoço")
 * @param quantity Quantidade de unidades adquiridas associadas a este tíquete
 * @param totalAmount Valor monetário total pago pelo item
 * @param buyerName Nome completo do comprador (estudante ou visitante)
 * @param buyerCpf CPF do comprador para conferência no balcão
 * @param buyerType Enquadramento do comprador ({@link BuyerType#STUDENT} ou {@link BuyerType#GUEST})
 * @param paidAt Data e hora exatas da compensação bancária e liquidação do tíquete
 *
 * @author Caio da Silva Viana
 */
public record TicketReadyResponseDTO(
    UUID itemId,
    UUID orderId,
    String productTitle,
    Integer quantity,
    BigDecimal totalAmount,
    String buyerName,
    String buyerCpf,
    BuyerType buyerType,
    Instant paidAt
) {
    /**
     * Construtor de conveniência que constrói a projeção a partir de uma entidade {@link OrderItem}.
     *
     * @param item A entidade de item de pedido quitada.
     */
    public TicketReadyResponseDTO(OrderItem item) {
        this(
            item.getId(),
            item.getOrder() != null ? item.getOrder().getId() : null,
            item.getProduct() != null ? item.getProduct().getTitle() : null,
            item.getQuantity(),
            item.getTotalAmount(),
            item.getOrder() != null ? item.getOrder().getBuyerName() : null,
            item.getOrder() != null ? item.getOrder().getBuyerCpf() : null,
            item.getOrder() != null ? item.getOrder().getBuyerType() : BuyerType.GUEST,
            item.getPaidAt()
        );
    }
}
