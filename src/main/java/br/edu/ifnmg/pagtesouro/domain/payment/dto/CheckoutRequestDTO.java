package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * DTO para capturar a solicitação de checkout, suportando tanto o fluxo direto/simplificado
 * (anônimo) quanto o fluxo robusto integrado ao Pedido (carrinho de compras).
 *
 * @author Caio da Silva Viana
 */
public record CheckoutRequestDTO(

    /**
     * ID opcional do pedido para o checkout integrado (carrinho).
     */
    @JsonProperty("orderId")
    @JsonAlias({"order_id", "orderId"})
    UUID orderId,

    /**
     * ID opcional do produto para o checkout direto/simplificado.
     */
    @JsonProperty("productID")
    @JsonAlias({"product_id", "productId", "productID"})
    UUID productID,

    /**
     * Quantidade opcional do produto para o checkout direto.
     */
    @JsonProperty("quantity")
    @JsonAlias("quantity")
    @Min(value = 1, message = "A quantidade mínima deve ser 1!")
    Integer quantity,

    /**
     * CPF ou CNPJ do pagador de fato (obrigatório para fins fiscais e de auditoria da STN).
     * Opcional se o usuário estiver autenticado (o backend obterá os dados do token de forma segura).
     */
    @JsonProperty("contributorCpfCnpj")
    @JsonAlias({"contributor_cpf_cnpj", "contributorCpfCnpj"})
    String contributorCpfCnpj,

    /**
     * Nome do contribuinte pagador (obrigatório para emissão da GRU no PagTesouro).
     * Opcional se o usuário estiver autenticado (o backend obterá os dados do token de forma segura).
     */
    @JsonProperty("contributorName")
    @JsonAlias({"contributor_name", "contributorName"})
    String contributorName,

    @JsonProperty("isMobile")
    @JsonAlias({"is_mobile", "isMobile"})
    boolean isMobile

) {}
