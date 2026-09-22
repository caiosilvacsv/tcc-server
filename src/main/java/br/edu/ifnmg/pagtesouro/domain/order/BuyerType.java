package br.edu.ifnmg.pagtesouro.domain.order;

import lombok.Getter;

/**
 * Enumeração que categoriza o tipo de comprador ou pagador de um pedido no ecossistema do IFNMG.
 * <p>
 * <b>Conceito no TCC (Portal de Débitos):</b>
 * Permite que a interface do Ponto de venda e a emissão de relatórios
 * distingam se o tíquete foi adquirido por um estudante/servidor devidamente autenticado
 * com conta no portal ou por um visitante/cidadão externo que realizou a compra de forma direta/anônima.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum BuyerType {

    /**
     * Comprador com vínculo acadêmico institucional ativo, autenticado via credenciais institucionais.
     */
    STUDENT("STUDENT"),

    /**
     * Comprador visitante ou externo sem vínculo ou conta no portal, que comprou via checkout direto.
     */
    GUEST("GUEST");

    private final String value;

    /**
     * Construtor do enum de enquadramento do comprador.
     *
     * @param value A representação textual do tipo de comprador.
     */
    BuyerType(String value) {
        this.value = value;
    }
}
