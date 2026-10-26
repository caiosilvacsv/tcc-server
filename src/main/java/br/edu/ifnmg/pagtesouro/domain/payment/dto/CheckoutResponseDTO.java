package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) contendo a resposta consolidada de uma solicitação de checkout bem-sucedida.
 * Encapsula o identificador de pagamento local, a URL segura de redirecionamento para o ecossistema do PagTesouro (STN)
 * e os metadados descritivos da guia para exibição direta no frontend (Server-Driven UI).
 *
 * @param paymentId Identificador único global (UUID) da transação gerada e persistida no banco de dados local
 * @param pagtesouroPaymentId Código identificador da solicitação de pagamento retornado externamente pelo PagTesouro
 * @param amount Valor financeiro consolidado cobrado nesta tentativa de pagamento
 * @param status Estado operacional inicial da transação cadastrada no banco de dados local (ex: "CREATED")
 * @param nextUrl URL de redirecionamento segura gerada pela STN para conclusão do pagamento na página do PagTesouro
 * @param codeService Código numérico do serviço SISGRU associado a esta guia de pagamento
 * @param serviceTitle Título descritivo da guia de pagamento para exibição direta ao usuário
 * @param itemsDescription Lista resumida dos itens de pedido incluídos nesta guia específica
 *
 * @author Caio da Silva Viana
 */
public record CheckoutResponseDTO(
    UUID paymentId,
    String pagtesouroPaymentId,
    BigDecimal amount,
    String status,
    String nextUrl,
    String codeService,
    String serviceTitle,
    List<String> itemsDescription
) {
    /**
     * Construtor de compatibilidade de 5 parâmetros.
     */
    public CheckoutResponseDTO(
        UUID paymentId,
        String pagtesouroPaymentId,
        BigDecimal amount,
        String status,
        String nextUrl
    ) {
        this(paymentId, pagtesouroPaymentId, amount, status, nextUrl, null, "Guia Oficial PagTesouro (STN)", List.of());
    }

    /**
     * Construtor de compatibilidade de 6 parâmetros.
     */
    public CheckoutResponseDTO(
        UUID paymentId,
        String pagtesouroPaymentId,
        BigDecimal amount,
        String status,
        String nextUrl,
        String codeService
    ) {
        this(
            paymentId,
            pagtesouroPaymentId,
            amount,
            status,
            nextUrl,
            codeService,
            codeService != null ? "Guia SISGRU #" + codeService : "Guia Oficial PagTesouro (STN)",
            List.of()
        );
    }
}
