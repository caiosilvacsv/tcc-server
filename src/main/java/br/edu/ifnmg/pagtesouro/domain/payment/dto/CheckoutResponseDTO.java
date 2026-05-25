package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) contendo a resposta consolidada de uma solicitação de checkout bem-sucedida.
 * Encapsula o identificador de pagamento local e a URL segura de redirecionamento para o ecossistema do PagTesouro (STN).
 * <p>
 * **Conceito no TCC:**
 * Modela a carga de dados reativa de saída que instrui o portal do estudante (frontend) a efetuar a transição visual,
 * redirecionando o usuário para a interface de liquidação (Pix/Boleto) da Secretaria do Tesouro Nacional.
 * </p>
 *
 * @param paymentId Identificador único global (UUID) da transação gerada e persistida no banco de dados local
 * @param pagtesouroPaymentId Código identificador da solicitação de pagamento retornado externamente pelo PagTesouro
 * @param amount Valor financeiro consolidado cobrado nesta tentativa de pagamento
 * @param status Estado operacional inicial da transação cadastrada no banco de dados local (ex: "CREATED")
 * @param nextUrl URL de redirecionamento segura gerada pela STN para conclusão do pagamento na página do PagTesouro
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
