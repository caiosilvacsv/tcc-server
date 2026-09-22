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
 * <p>
 * **Conceito no TCC (Auditoria e Transparência):**
 * Modela as respostas das rotas REST de consulta de histórico, permitindo ao estudante ou doador
 * acompanhar o status de compensação das faturas do PagTesouro (Pix, Boleto, etc.).
 * </p>
 *
 * @param id Identificador único (UUID) interno da cobrança
 * @param referenceNumber Número de referência estritamente numérico gerado pelo Postgres
 * @param pagtesouroPaymentId ID externo da transação retornado pela STN
 * @param amount Valor total consolidado em centavos/decimal
 * @param status Situação atual da transação (ex: CREATED, COMPLETED)
 * @param paymentMethod Meio de pagamento processado (ex: PIX, BOLETO, CARTAO)
 * @param contributorName Nome do pagador cadastrado na GRU
 * @param contributorCpfCnpj CPF/CNPJ de faturamento
 * @param nextUrl URL de redirecionamento ou segunda via da cobrança
 * @param createdAt Carimbo de data/hora de geração da cobrança
 * @param paidAt Carimbo de data/hora de confirmação da compensação pelo banco
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
    Instant paidAt
) {
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
            payment.getPaidAt()
        );
    }
}
