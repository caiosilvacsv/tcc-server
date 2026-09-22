package br.edu.ifnmg.pagtesouro.domain.product;

import lombok.Getter;

/**
 * Enum que define as categorias de enquadramento dos produtos e serviços no sistema.
 * <p>
 * **Conceito no TCC:**
 * Permite categorizar os itens cobráveis para fins de relatórios e separação lógica
 * de regras de negócio (ex: o que são tíquetes consumíveis de refeitório vs. o que são
 * taxas/multas administrativas).
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum ProductCategory {

    /**
     * Categoria para Tíquetes físicos de alimentação (Almoço, Janta, Lanche) do refeitório.
     */
    TICKET("ticket", "Tíquete de Alimentação"),

    /**
     * Categoria para taxas e multas administrativas (como multas de atraso de livros da Biblioteca).
     */
    FINE("fine", "Taxas e Multas Administrativas");

    private final String value;
    private final String displayName;

    ProductCategory(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    /**
     * Converte uma string para o Enum correspondente de forma segura e validada.
     *
     * @param value O valor em formato string (ex: "ticket", "fine")
     * @return O Enum {@link ProductCategory} correspondente
     * @throws IllegalArgumentException se a string não corresponder a nenhuma categoria cadastrada
     */
    public static ProductCategory fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Categoria não pode ser nula");
        }
        for (ProductCategory category : ProductCategory.values()) {
            if (category.getValue().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        throw new IllegalArgumentException("Categoria de produto inválida: " + value);
    }
}

