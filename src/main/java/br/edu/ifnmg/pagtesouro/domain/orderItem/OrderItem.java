package br.edu.ifnmg.pagtesouro.domain.orderItem;

import br.edu.ifnmg.pagtesouro.domain.order.Order;
import br.edu.ifnmg.pagtesouro.domain.product.MoneyToCentsConverter;
import br.edu.ifnmg.pagtesouro.domain.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA que representa um item individual pertencente a um pedido (Order).
 * <p>
 * **Conceito no TCC (Portal de Débitos):**
 * Modela os tíquetes de refeição adquiridos ou as multas de biblioteca em processo de quitação.
 * Cada item possui seu próprio ciclo de vida de pagamento e liberação, permitindo agrupamentos
 * flexíveis no momento de gerar a cobrança (GRU/Pix) junto ao PagTesouro.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order_item")
@Entity(name = "order_item")
public class OrderItem {

    /**
     * Identificador único do item no banco de dados.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Relacionamento bidirecional com o pedido principal (cabeçalho).
     * Mapeado como 'order' para alinhar ao atributo mappedBy de Order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * O produto (tíquete ou multa) associado a este item de cobrança.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Quantidade de itens adquiridos (geralmente 1 para multas de biblioteca).
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Valor total deste item (quantidade * preço unitário do produto) em centavos.
     * Mapeado usando o conversor seguro BigDecimal-Inteiro.
     */
    @Column(name = "total_amount", nullable = false)
    @Convert(converter = MoneyToCentsConverter.class)
    private BigDecimal totalAmount;

    /**
     * Data e hora exatas de compensação bancária e liquidação deste item específico.
     */
    @Column(name = "paid_at")
    private Instant paidAt;

    /**
     * Data e hora exatas em que o item físico foi retirado ou trocado pelo ADMIN.
     */
    @Column(name = "exchanged_at")
    private Instant exchangedAt;

    /**
     * Identificador do ADMIN que efetuou a baixa/troca física deste item.
     */
    @Column(name = "exchanged_by")
    private UUID exchangedBy;

    /**
     * Estado atual de pagamento deste item individual.
     * Mapeado como String para facilitar auditorias diretas no banco de dados.
     *
     * @see OrderItemStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderItemStatus status = OrderItemStatus.PENDING;
}

