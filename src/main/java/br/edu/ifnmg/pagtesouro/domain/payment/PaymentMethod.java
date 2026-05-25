package br.edu.ifnmg.pagtesouro.domain.payment;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enum que define os meios de pagamento homologados e processados via API do PagTesouro.
 * <p>
 * **Conceito no TCC:**
 * Representa a forma de liquidação escolhida pelo contribuinte (estudante/cidadão)
 * no Portal de Débitos do IFNMG.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum PaymentMethod {

    /**
     * Pix: Meio de pagamento instantâneo do Banco Central.
     * Retorna a confirmação de liquidação em poucos segundos.
     */
    PIX("PIX"),

    /**
     * Cartão de Crédito: Permite parcelamentos de taxas acadêmicas.
     */
    CREDIT_CARD("CARTAO_CREDITO"),

    /**
     * Saldo em Carteira Digital (ex: Mercado Pago, PicPay, carteiras homologadas pelo governo).
     */
    WALLET_BALANCE("SALDO_CARTEIRA"),

    /**
     * Boleto Bancário (Guia de Recolhimento da União - GRU Simples ou Cobrança).
     */
    GRU("BOLETO");

    @JsonValue
    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    /**
     * Converte e valida uma string representativa do método de pagamento para o Enum correspondente de forma segura.
     *
     * @param value O texto correspondente ao método de pagamento (ex: "PIX", "CARTAO_CREDITO", "BOLETO")
     * @return O Enum {@link PaymentMethod} correspondente
     * @throws IllegalArgumentException se o valor fornecido for nulo ou inválido
     */
    public static PaymentMethod fromMethod(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Meio de pagamento não pode ser nulo");
        }
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.getValue().equalsIgnoreCase(value.trim()) ||
                method.name().equalsIgnoreCase(value.trim())) {
                return method;
            }
        }
        throw new IllegalArgumentException("Meio de pagamento inválido: " + value);
    }
}

