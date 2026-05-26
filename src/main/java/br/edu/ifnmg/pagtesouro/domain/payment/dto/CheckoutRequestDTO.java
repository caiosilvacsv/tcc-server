package br.edu.ifnmg.pagtesouro.domain.payment.dto;

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
    UUID orderId,

    /**
     * ID opcional do produto para o checkout direto/simplificado.
     */
    UUID productID,

    /**
     * Quantidade opcional do produto para o checkout direto.
     */
    @Min(value = 1, message = "A quantidade mínima deve ser 1!")
    Integer quantity,

    /**
     * CPF ou CNPJ do pagador de fato (obrigatório para fins fiscais e de auditoria da STN).
     */
    @NotBlank(message = "O CPF/CNPJ do pagador é obrigatório!")
    String contributorCpfCnpj,

    /**
     * Nome do contribuinte pagador (obrigatório para emissão da GRU no PagTesouro).
     */
    @NotBlank(message = "O nome do pagador é obrigatório!")
    String contributorName,

    boolean isMobile

) {}
