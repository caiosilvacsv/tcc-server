package br.edu.ifnmg.pagtesouro.domain.product.dto;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;

/**
 * Objeto de Transferência de Dados (DTO) para expor as categorias de produtos disponíveis no sistema.
 *
 * @param key O identificador estático do Enum (ex: "TICKET")
 * @param value O valor amigável em formato textual/slug (ex: "ticket")
 * @param displayName O nome legível para exibição em interfaces de usuário (ex: "Tíquete de Alimentação")
 *
 * @author Caio da Silva Viana
 */
public record ProductCategoryResponseDTO(
    String key,
    String value,
    String displayName
) {
    public ProductCategoryResponseDTO(ProductCategory category) {
        this(category.name(), category.getValue(), category.getDisplayName());
    }
}
