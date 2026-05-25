package br.edu.ifnmg.pagtesouro.domain.order.dto;

import java.util.List;
import java.util.UUID;

public record OrderRequestDTO(
    List<items> itemsList
) {
    public record items(
        UUID productId,
        Integer quantity
    ){}
}
