package br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) contendo os dados de envio para a solicitação de pagamento no PagTesouro.
 * Mapeia os atributos em inglês para os campos em português exigidos oficialmente pela API do PagTesouro.
 *
 * @author Caio da Silva Viana
 */
public record PagTesouroRequestDTO(
    @JsonProperty("codigoServico")
    Integer codeService,

    @JsonProperty("referencia")
    String reference,

    @JsonProperty("competencia")
    Integer competence,

    @JsonProperty("vencimento")
    String expireAt, // String no formato DDMMAAAA exigido pela STN

    @JsonProperty("cnpjCpf")
    String contributorCpfCnpj,

    @JsonProperty("nomeContribuinte")
    String contributorName,

    @JsonProperty("valorPrincipal")
    BigDecimal principalAmount,

    @JsonProperty("valorDescontos")
    BigDecimal discountAmount,

    @JsonProperty("valorOutrasDeducoes")
    BigDecimal deductionsAmount,

    @JsonProperty("valorMulta")
    BigDecimal fineAmount,

    @JsonProperty("valorJuros")
    BigDecimal interestAmount,

    @JsonProperty("valorOutrosAcrescimos")
    BigDecimal additionsAmount,

    @JsonProperty("modoNavegacao")
    Integer navigateMode,

    @JsonProperty("urlNotificacao")
    String notificationUrl
) {
}
