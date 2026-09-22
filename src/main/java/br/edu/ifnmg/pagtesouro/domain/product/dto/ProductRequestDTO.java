package br.edu.ifnmg.pagtesouro.domain.product.dto;

import br.edu.ifnmg.pagtesouro.domain.product.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) responsável por receber os dados de entrada na criação ou atualização de um Produto.
 * Inclui anotações de validação do Bean Validation para garantir a integridade dos dados de entrada.
 *
 * @author Caio da Silva Viana
 */
public record ProductRequestDTO(
    @NotBlank(message = "O título do produto é obrigatório.")
    String title,

    String description,

    String image,

    @NotNull(message = "O preço do produto é obrigatório.")
    @PositiveOrZero(message = "O preço do produto não pode ser negativo.")
    BigDecimal price,

    boolean active,

    @NotBlank(message = "O código do serviço no SISGRU é obrigatório.")
    String codeService,

    @NotNull(message = "A categoria do produto é obrigatória.")
    ProductCategory category
) {}
