package br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto;

import br.edu.ifnmg.pagtesouro.domain.payment.PaymentMethod;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO contendo a resposta detalhada de consulta de pagamento retornada pelo PagTesouro.
 * Mapeia os atributos da STN em português para o domínio em inglês de forma resiliente.
 *
 * @author Caio da Silva Viana
 */
public record PagTesouroQueryResponseDTO(
    @JsonProperty("idPagamento")
    String idPayment,

    @JsonProperty("tipoPagamentoEscolhido")
    PaymentMethod typePayment,

    @JsonProperty("valor")
    BigDecimal amount,

    @JsonProperty("nomePSP")
    String paymentServiceProviderName,

    @JsonProperty("transacaoPSP")
    String pspTransactionId,

    @JsonProperty("situacao")
    Situation situation
) {
    /**
     * Situação do pagamento detalhada.
     */
    public record Situation(
        @JsonProperty("codigo")
        PaymentStatus code,

        @JsonProperty("data")
        Instant date
    ){}
}
