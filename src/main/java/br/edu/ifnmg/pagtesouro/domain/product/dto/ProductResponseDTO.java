package br.edu.ifnmg.pagtesouro.domain.product.dto;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) responsável por expor os detalhes formatados de um Produto nas respostas da API.
 *
 * @author Caio da Silva Viana
 */
public record ProductResponseDTO(
    UUID id,
    String title,
    String description,
    String image,
    BigDecimal price,
    boolean active,
    String codeService,
    ProductCategory category
) {
    /**
     * Construtor auxiliar para mapeamento direto de uma entidade {@link Product} para DTO.
     *
     * @param product A entidade de Produto original.
     */
    public ProductResponseDTO(Product product) {
        this(
            product.getId(),
            product.getTitle(),
            product.getDescription(),
            product.getImage(),
            product.getPrice(),
            product.isActive(),
            product.getCodeService(),
            product.getCategory()
        );
    }
}
