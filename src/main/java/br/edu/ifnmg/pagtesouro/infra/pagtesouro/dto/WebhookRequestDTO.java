package br.edu.ifnmg.pagtesouro.infra.pagtesouro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Objeto de Transferência de Dados (DTO) minimalista projetado para capturar as notificações
 * ativas de status de pagamento (Webhook/notificacao-pagamento) enviadas de forma assíncrona
 * pelos servidores da Secretaria do Tesouro Nacional (STN).
 * <p>
 * **Conceito no TCC (Fluxo Push Failsafe):**
 * Modela a carga de dados mínima requerida para acionar de forma imediata o motor de conciliação do backend.
 * Assim que recebido o webhook com a chave de transação do PagTesouro, o sistema local dispara uma consulta de
 * confirmação de segurança (Failsafe) antes de liquidar definitivamente os saldos dos itens.
 * </p>
 *
 * @param idPayment Identificador único da transação gerado originalmente pelo PagTesouro (exigido para cruzamento de dados)
 * @param dateTime String contendo o registro de data e hora do processamento da liquidação no banco de origem
 *
 * @author Caio da Silva Viana
 */
public record WebhookRequestDTO(
    @JsonProperty("idPagamento")
    String idPayment,

    @JsonProperty("dataHora")
    String dateTime
) {}

