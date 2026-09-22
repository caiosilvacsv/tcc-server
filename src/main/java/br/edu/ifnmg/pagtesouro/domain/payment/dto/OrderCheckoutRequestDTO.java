package br.edu.ifnmg.pagtesouro.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para solicitação de checkout de um Pedido (carrinho) por um estudante autenticado.
 * Todos os campos fiscais são opcionais, pois o backend utiliza automaticamente
 * o nome completo e o CPF do usuário autenticado como fallback padrão (LGPD-compliant).
 *
 * @param contributorCpfCnpj CPF/CNPJ opcional para emissão da GRU (caso outra pessoa vá pagar)
 * @param contributorName Nome opcional do contribuinte para a GRU (caso outra pessoa vá pagar)
 * @param isMobile Define se o checkout será renderizado em modo mobile ou desktop
 *
 * @author Caio da Silva Viana
 */
public record OrderCheckoutRequestDTO(
    @JsonProperty("contributorCpfCnpj")
    @JsonAlias({"contributor_cpf_cnpj", "contributorCpfCnpj"})
    String contributorCpfCnpj,

    @JsonProperty("contributorName")
    @JsonAlias({"contributor_name", "contributorName"})
    String contributorName,

    @JsonProperty("isMobile")
    @JsonAlias({"is_mobile", "isMobile"})
    boolean isMobile
) {
    @JsonCreator
    public OrderCheckoutRequestDTO(
        @JsonProperty("contributorCpfCnpj") String contributorCpfCnpj,
        @JsonProperty("contributorName") String contributorName,
        @JsonProperty(value = "isMobile", defaultValue = "false") boolean isMobile
    ) {
        this.contributorCpfCnpj = contributorCpfCnpj;
        this.contributorName = contributorName;
        this.isMobile = isMobile;
    }

    /**
     * Construtor de conveniência para checkout padrão rápido.
     */
    public OrderCheckoutRequestDTO(boolean isMobile) {
        this(null, null, isMobile);
    }
}
