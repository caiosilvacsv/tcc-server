package br.edu.ifnmg.pagtesouro.domain.payment;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enum que define a situação/status de um Pagamento no ciclo de vida da API do PagTesouro.
 * <p>
 * **Conceito no TCC:**
 * Mapeia o ciclo assíncrono de transações do PagTesouro. Cada status representa o progresso
 * desde o momento em que a fatura/guia de cobrança é gerada pelo backend até a sua liquidação
 * final (notificada via Webhook) ou cancelamento por expiração.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum PaymentStatus {

    /**
     * O pagamento foi solicitado pelo sistema cliente local (tcc-server), 
     * mas a URL de pagamento do iFrame (proximaUrl) ainda não foi acessada pelo aluno.
     */
    CREATED("CRIADO"),

    /**
     * O aluno abriu o iFrame e iniciou a seleção do método de pagamento (Pix, Cartão ou GRU).
     */
    STARTED("INICIADO"),

    /**
     * O boleto (GRU) foi gerado ou o Pix foi apresentado ao usuário, aguardando a compensação bancária.
     */
    SUBMITTED("SUBMETIDO"),

    /**
     * O pagamento foi concluído e compensado com sucesso na conta única do Tesouro Nacional.
     * **Ação no TCC:** Este status dispara a liberação digital automática dos tíquetes para o aluno.
     */
    COMPLETED("CONCLUIDO"),

    /**
     * O pagamento foi rejeitado (ex: transação de cartão de crédito negada ou falha operacional).
     */
    REJECTED("REJEITADO"),

    /**
     * O pagamento expirou por tempo limite (ex: Pix não pago no prazo) ou foi cancelado.
     */
    CANCELLED("CANCELADO");

    @JsonValue
    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    /**
     * Converte e valida uma string de status para o Enum correspondente de forma segura.
     *
     * @param status O nome do status textual (ex: "CRIADO", "CONCLUIDO")
     * @return O Enum {@link PaymentStatus} correspondente
     * @throws IllegalArgumentException se o status fornecido for inválido
     */
    public static PaymentStatus fromStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status de pagamento não pode ser nulo");
        }
        for (PaymentStatus paymentStatus : PaymentStatus.values()) {
            if (paymentStatus.getStatus().equalsIgnoreCase(status.trim())) {
                return paymentStatus;
            }
        }
        throw new IllegalArgumentException("Status de pagamento inválido: " + status);
    }
}

