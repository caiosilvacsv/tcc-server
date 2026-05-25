package br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO minimalista para capturar a notificação de webhook do PagTesouro.
 */
public record WebhookRequestDTO(
    @JsonProperty("idPagamento")
    String idPayment,

    @JsonProperty("dataHora")
    String dateTime
) {}
