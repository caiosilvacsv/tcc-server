package br.edu.ifnmg.pagtesouro.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) para solicitação de baixa física (resgate) em lote de tíquetes.
 * <p>
 * <b>Conceito no TCC:</b>
 * Permite que o funcionário administrador no balcão de atendimento selecione
 * um ou múltiplos tíquetes na interface e processe a entrega de forma atômica e simultânea em uma única requisição.
 * </p>
 *
 * @param itemIds Lista contendo os identificadores únicos (UUIDs) dos itens de pedido a serem baixados
 *
 * @author Caio da Silva Viana
 */
public record BatchExchangeRequestDTO(
    @NotEmpty(message = "A lista de itens para baixa não pode ser vazia!")
    @JsonProperty("itemIds")
    @JsonAlias({"item_ids", "itemIds", "items"})
    List<UUID> itemIds
) {}
