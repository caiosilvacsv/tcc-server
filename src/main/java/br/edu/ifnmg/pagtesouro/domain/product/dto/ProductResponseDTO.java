package br.edu.ifnmg.pagtesouro.domain.product.dto;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) encarregado de expor os detalhes estruturados e formatados
 * de um produto ou serviço ativo nas respostas das interfaces públicas e privadas da API REST.
 *
 * @author Caio da Silva Viana
 */
public record ProductResponseDTO(
    UUID id,
    String title,
    String description,
    String imageUrl,
    String color,
    BigDecimal price,
    boolean active,
    String codeService,
    ProductCategory category
) {
    public ProductResponseDTO(Product product) {
        this(
            product.getId(),
            product.getTitle(),
            product.getDescription(),
            product.getImageUrl(),
            product.getColor(),
            product.getPrice(),
            product.isActive(),
            product.getCodeService(),
            product.getCategory()
        );
    }

    public ProductResponseDTO(UUID id, String title, String description, String image, BigDecimal price, boolean active, String codeService, ProductCategory category) {
        this(id, title, description, image, null, price, active, codeService, category);
    }

    public String image() {
        return this.imageUrl;
    }
}
