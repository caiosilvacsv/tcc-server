package br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto;

import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * DTO contendo a resposta retornada pela API do PagTesouro após uma solicitação de cobrança.
 *
 * @author Caio da Silva Viana
 */
public record PagTesouroResponseDTO(
    @JsonProperty("idPagamento")
    String idPayment,

    @JsonProperty("dataCriacao")
    Instant created_at,

    @JsonProperty("proximaUrl")
    String nextUrl,

    @JsonProperty("situacao")
    Situacao situation
) {
    public record Situacao(
      @JsonProperty("codigo")
      PaymentStatus code
    ){}
}
