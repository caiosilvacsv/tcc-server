package br.edu.ifnmg.pagtesouro.domain.product.dto;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) encarregado de expor os detalhes estruturados e formatados
 * de um produto ou serviço ativo nas respostas das interfaces públicas e privadas da API REST.
 * <p>
 * **Conceito no TCC:**
 * Modela a exibição legível de itens cobráveis do catálogo acadêmico (ex: guias de recolhimento de biblioteca, RU,
 * certificados etc.), permitindo que o frontend exiba os preços convertidos e os códigos de serviços correspondentes no SISGRU.
 * </p>
 *
 * @param id Identificador único global (UUID) do produto no banco de dados local
 * @param title Nome comercial ou título curto descritivo do produto/serviço (ex: "Almoço - Refeitório Universitário")
 * @param description Explicação detalhada acerca dos critérios de aquisição ou regras de cobrança do serviço
 * @param image URL pública de localização da imagem/ícone representativo do produto para fins estéticos de catálogo
 * @param price Valor unitário cobrado pelo produto/serviço em formato decimal preciso (BigDecimal)
 * @param active Indicador lógico se o item está ativo no catálogo para aquisições (Soft Delete/Desativação física)
 * @param codeService Código de subgrupamento no SISGRU/STN para liquidação financeira do serviço
 * @param category Categoria de enquadramento estrutural do serviço (ex: RU, BIBLIOTECA, OUTROS)
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
