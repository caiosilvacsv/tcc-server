package br.edu.ifnmg.pagtesouro.domain.order;

import lombok.Getter;

/**
 * Enum que define os estados possíveis do ciclo de vida de um pedido (Order).
 * <p>
 * **Conceito no TCC (Portal de Débitos):**
 * Modela o estado consolidado da transação do estudante. Como um pedido pode conter múltiplos
 * itens de cobrança associados a diferentes códigos de serviço da STN, o status do pedido reflete
 * a agregação do estado de seus itens (OrderItem).
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum OrderStatus {

    /**
     * O pedido (carrinho de cobrança) foi criado e gravado no sistema local,
     * mas nenhuma tentativa de pagamento junto à API do PagTesouro foi gerada ainda.
     */
    CREATED("criado"),

    /**
     * Uma ou mais tentativas de pagamento (Pix/Boleto) foram disparadas junto à STN,
     * e o sistema aguarda a quitação ou retorno dos webhooks.
     */
    PENDING_PAYMENT("pagamento pendente"),

    /**
     * Estado especial no TCC: ocorre quando o pedido continha múltiplos itens com diferentes
     * códigos de serviço (ex: tíquetes de RU + multas de biblioteca), e o aluno realizou o pagamento
     * bem-sucedido de apenas uma parte das transações geradas, mantendo o restante pendente.
     */
    PARTIALLY_PAID("parcialmente pago"),

    /**
     * Todos os itens vinculados ao pedido foram completamente confirmados como pagos e liquidados
     * pelo retorno dos webhooks do PagTesouro.
     */
    COMPLETED("concluido"),

    /**
     * O pedido foi cancelado por expiração dos prazos de transação (Pix expirados)
     * ou desistência voluntária do usuário.
     */
    CANCELLED("cancelado");

    private final String status;

    OrderStatus(String status) {
        this.status = status;
    }

    /**
     * Converte e valida uma string de status para o Enum correspondente de forma segura.
     *
     * @param status O texto do status (ex: "criado", "pagamento pendente", "concluido", "cancelado")
     * @return O Enum {@link OrderStatus} correspondente
     * @throws IllegalArgumentException se o status fornecido for nulo ou inválido
     */
    public static OrderStatus fromStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status do pedido não pode ser nulo");
        }
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.getStatus().equalsIgnoreCase(status.trim()) ||
                orderStatus.name().equalsIgnoreCase(status.trim())) {
                return orderStatus;
            }
        }
        throw new IllegalArgumentException("Status do pedido inválido: " + status);
    }
}

