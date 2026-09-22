package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO para solicitação de checkout direto/simplificado (anônimo) de um único produto.
 * Exige todos os dados cadastrais e fiscais do pagador, pois não há usuário autenticado na sessão.
 *
 * @param productId Identificador único do produto ou serviço acadêmico
 * @param quantity Quantidade de unidades solicitadas (mínimo 1)
 * @param contributorCpfCnpj CPF ou CNPJ do pagador (obrigatório para emissão da GRU)
 * @param contributorName Nome completo do contribuinte pagador
 * @param isMobile Define se o checkout será renderizado em modo mobile ou desktop
 *
 * @author Caio da Silva Viana
 */
public record DirectCheckoutRequestDTO(
    @NotNull(message = "O ID do produto é obrigatório para checkout direto!")
    @JsonProperty("productId")
    @JsonAlias({"product_id", "productID", "productId"})
    UUID productId,

    @NotNull(message = "A quantidade é obrigatória!")
    @Min(value = 1, message = "A quantidade mínima deve ser 1!")
    @JsonProperty("quantity")
    @JsonAlias("quantity")
    Integer quantity,

    @NotBlank(message = "O CPF/CNPJ do pagador é obrigatório para checkout simplificado!")
    @JsonProperty("contributorCpfCnpj")
    @JsonAlias({"contributor_cpf_cnpj", "contributorCpfCnpj"})
    String contributorCpfCnpj,

    @NotBlank(message = "O nome do pagador é obrigatório para checkout simplificado!")
    @JsonProperty("contributorName")
    @JsonAlias({"contributor_name", "contributorName"})
    String contributorName,

    @JsonProperty("isMobile")
    @JsonAlias({"is_mobile", "isMobile"})
    boolean isMobile
) {}
