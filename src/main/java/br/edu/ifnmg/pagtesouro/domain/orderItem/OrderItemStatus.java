package br.edu.ifnmg.pagtesouro.domain.orderItem;

import lombok.Getter;

/**
 * Enum que representa o ciclo de vida e estado de pagamento de um item individual do pedido.
 * <p>
 * **Conceito no TCC (Portal de Débitos):**
 * Ao contrário do pedido (Order) que atua como um cabeçalho de carrinho agrupador, o status do
 * pagamento e liberação é controlado de forma granular por item (OrderItem). Isso permite que o
 * aluno pague itens de diferentes códigos de serviço de forma independente em transações separadas
 * do PagTesouro.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum OrderItemStatus {

    /**
     * O item de serviço/tíquete foi solicitado pelo estudante, mas a transação do PagTesouro
     * correspondente ainda não foi confirmada ou liquidada.
     */
    PENDING("PENDENTE"),

    /**
     * O item teve seu pagamento confirmado via Webhook da STN e está liberado para
     * consumo ou quitação definitiva de débitos na biblioteca.
     */
    PAID("PAGO"),

    /**
     * O item foi retirado em mãos ou trocado de forma definitiva pelo ADMIN no balcão de atendimento.
     */
    EXCHANGED("TROCADO"),

    /**
     * O item foi cancelado devido à expiração do prazo de pagamento (Pix expirado)
     * ou desistência manual do usuário.
     */
    CANCELLED("CANCELADO");

    private final String status;

    OrderItemStatus(String status) {
        this.status = status;
    }

    /**
     * Converte e valida uma string de status para o Enum correspondente de forma segura.
     *
     * @param status O texto representativo do status (ex: "PENDENTE", "PAGO", "CANCELADO")
     * @return O Enum {@link OrderItemStatus} correspondente
     * @throws IllegalArgumentException se o status fornecido for nulo ou inválido
     */
    public static OrderItemStatus fromStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do item de pedido não pode ser nulo");
        }
        for (OrderItemStatus itemStatus : OrderItemStatus.values()) {
            if (itemStatus.getStatus().equalsIgnoreCase(status.trim()) ||
                itemStatus.name().equalsIgnoreCase(status.trim())) {
                return itemStatus;
            }
        }
        throw new IllegalArgumentException("Status do item de pedido inválido: " + status);
    }
}

