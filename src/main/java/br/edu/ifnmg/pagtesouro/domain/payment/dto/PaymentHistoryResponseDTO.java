package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentMethod;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) para representação detalhada e paginada
 * do histórico de tentativas e transações de pagamento realizadas por um contribuinte.
 *
 * @author Caio da Silva Viana
 */
public record PaymentHistoryResponseDTO(
    UUID id,
    Long referenceNumber,
    String pagtesouroPaymentId,
    BigDecimal amount,
    PaymentStatus status,
    PaymentMethod paymentMethod,
    String contributorName,
    String contributorCpfCnpj,
    String nextUrl,
    Instant createdAt,
    Instant paidAt,
    UUID orderId,
    String statusDescription,
    String paymentMethodDescription
) {
    /**
     * Construtor de compatibilidade de 11 parâmetros.
     */
    public PaymentHistoryResponseDTO(
        UUID id,
        Long referenceNumber,
        String pagtesouroPaymentId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        String contributorName,
        String contributorCpfCnpj,
        String nextUrl,
        Instant createdAt,
        Instant paidAt
    ) {
        this(
            id,
            referenceNumber,
            pagtesouroPaymentId,
            amount,
            status,
            paymentMethod,
            contributorName,
            contributorCpfCnpj,
            nextUrl,
            createdAt,
            paidAt,
            null,
            resolveStatusDescription(status),
            resolveMethodDescription(paymentMethod)
        );
    }

    /**
     * Construtor de conveniência que converte a entidade JPA {@link Payment} em sua projeção DTO.
     *
     * @param payment A entidade física JPA de pagamento resgatada do banco
     */
    public PaymentHistoryResponseDTO(Payment payment) {
        this(
            payment.getId(),
            payment.getReferenceNumber(),
            payment.getPagtesouroPaymentId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getPaymentMethod(),
            payment.getContributorName(),
            payment.getContributorCpfCnpj(),
            payment.getNextUrl(),
            payment.getCreatedAt(),
            payment.getPaidAt(),
            extractOrderId(payment),
            resolveStatusDescription(payment.getStatus()),
            resolveMethodDescription(payment.getPaymentMethod())
        );
    }

    private static UUID extractOrderId(Payment payment) {
        if (payment.getOrderItems() != null && !payment.getOrderItems().isEmpty()) {
            var firstItem = payment.getOrderItems().get(0);
            if (firstItem != null && firstItem.getOrder() != null) {
                return firstItem.getOrder().getId();
            }
        }
        return null;
    }

    private static String resolveStatusDescription(PaymentStatus status) {
        if (status == null) return "Aguardando";
        return switch (status) {
            case CREATED -> "Sessão Criada";
            case STARTED -> "Iniciado no PagTesouro";
            case SUBMITTED -> "Submetido (Aguardando Compensação)";
            case COMPLETED -> "Liquidado / Confirmado";
            case REJECTED -> "Rejeitado";
            case CANCELLED -> "Cancelado / Sessão Substituída";
        };
    }

    private static String resolveMethodDescription(PaymentMethod method) {
        if (method == null) return "A definir no PagTesouro";
        return switch (method) {
            case PIX -> "PIX Instantâneo";
            case CREDIT_CARD -> "Cartão de Crédito";
            case GRU -> "Boleto GRU";
            case WALLET_BALANCE -> "Saldo em Carteira";
        };
    }
}
