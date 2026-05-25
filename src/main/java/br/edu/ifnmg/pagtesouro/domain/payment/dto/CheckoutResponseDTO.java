package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO contendo o retorno de uma solicitação de checkout iniciada com sucesso.
 * Retorna o identificador de pagamento local e a URL de redirecionamento (nextUrl) da STN.
 *
 * @author Caio da Silva Viana
 */
public record CheckoutResponseDTO(
    UUID paymentId,
    String pagtesouroPaymentId,
    BigDecimal amount,
    String status,
    String nextUrl
) {}
