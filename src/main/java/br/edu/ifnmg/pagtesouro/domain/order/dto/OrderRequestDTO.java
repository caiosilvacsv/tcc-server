package br.edu.ifnmg.pagtesouro.domain.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * Objeto de Transferência de Dados (DTO) utilizado para receber requisições de criação
 * de novos pedidos (carrinhos de compras) originadas pela interface do estudante.
 * <p>
 * **Conceito no TCC:**
 * Representa o estado transacional inicial de uma intenção de pagamento de múltiplos débitos,
 * encapsulando a listagem granular de itens solicitados para consolidação em uma única fatura.
 * </p>
 *
 * @param itemsList Lista contendo as especificações detalhadas de cada item solicitado no pedido
 *
 * @author Caio da Silva Viana
 */
public record OrderRequestDTO(
    @JsonProperty("itemsList")
    @JsonAlias({"items_list", "itemsList"})
    List<items> itemsList
) {
    /**
     * Registro interno (record) que representa um item individual pertencente à lista de requisição do pedido.
     *
     * @param productId Identificador único global (UUID) do produto/serviço acadêmico desejado
     * @param quantity Quantidade inteira de unidades solicitadas do referido produto
     */
    public record items(
        @JsonProperty("productId")
        @JsonAlias({"product_id", "productId"})
        UUID productId,

        @JsonProperty("quantity")
        @JsonAlias("quantity")
        Integer quantity
    ){}
}

