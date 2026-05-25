package br.edu.ifnmg.pagtesouro.domain.payment;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import br.edu.ifnmg.pagtesouro.domain.product.MoneyToCentsConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA que representa uma tentativa ou registro de Pagamento junto à API do PagTesouro.
 * <p>
 * **Conceito no TCC:**
 * Modela os faturamentos de cobrança enviados à STN. Um único pedido (Order) pode possuir mais de
 * um pagamento associado (split de serviços ou novas tentativas caso um Pix anterior expire).
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payments")
@Entity(name = "payments")
public class Payment {

    /**
     * Identificador interno da cobrança gerado pelo backend.
     * Enviado como o parâmetro {@code idPagamento} (Identificador da Solicitação) para o PagTesouro.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Número de referência sequencial e estritamente numérico gerado pelo PostgreSQL
     * enviado como o parâmetro 'referencia' exigido pela API do PagTesouro.
     */
    @Column(name = "reference_number", unique = true, insertable = false, updatable = false)
    private Long referenceNumber;

    /**
     * ID de identificação retornado na resposta da API da STN (identificacaoSolicitacao).
     * Permite NULL inicial até que o POST no PagTesouro retorne com sucesso.
     */
    @Column(name = "pagtesouro_payment_id", length = 100, unique = true)
    private String pagtesouroPaymentId;

    /**
     * Número de controle de referência de cobrança opcional configurado no SISGRU.
     */
    private String reference;

    /**
     * Mês e ano de competência da cobrança (formato MMYYYY).
     */
    private Integer competence;

    /**
     * Data limite de vencimento da cobrança (GRU).
     */
    @Column(name = "expired_at")
    private LocalDate expiredAt;

    /**
     * Valor total consolidado da cobrança (incluindo principal, multa e juros) em centavos.
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * Situação atual da transação no ciclo de vida do PagTesouro.
     * Mapeado como String legível no banco de dados.
     *
     * @see PaymentStatus
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    /**
     * Define se a transação está ativa no histórico (controle de exclusão lógica).
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Método de pagamento escolhido pelo contribuinte (Pix, Cartão de Crédito, Boleto).
     * Mapeado como String legível no banco de dados.
     *
     * @see PaymentMethod
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Nome do Prestador de Serviços de Pagamento (Banco processador, ex: "Banco do Brasil").
     */
    @Column(name = "psp_name", length = 50)
    private String pspName;

    /**
     * Código de autenticação gerado pelo PSP/Banco da transação.
     */
    @Column(name = "psp_transaction_id", length = 50)
    private String pspTransactionId;

    /**
     * Valor principal do serviço/tíquete em centavos.
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    /**
     * Descontos ou abatimentos aplicados em centavos.
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Outras deduções da receita aplicadas em centavos.
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(name = "deductions_amount")
    private BigDecimal deductionsAmount = BigDecimal.ZERO;

    /**
     * Multa cobrada por atraso em centavos (útil em taxas de biblioteca).
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(name = "fine_amount")
    private BigDecimal fineAmount = BigDecimal.ZERO;

    /**
     * Juros de mora aplicados sobre a cobrança em centavos.
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(name = "interest_amount")
    private BigDecimal interestAmount = BigDecimal.ZERO;

    /**
     * Outros acréscimos legais aplicados em centavos.
     */
    @Convert(converter = MoneyToCentsConverter.class)
    @Column(name = "additions_amount")
    private BigDecimal additionsAmount = BigDecimal.ZERO;

    /**
     * CPF ou CNPJ de quem de fato realizou o pagamento (pode diferir do aluno).
     */
    @Column(name = "contributor_cpf_cnpj", length = 14)
    private String contributorCpfCnpj;

    /**
     * Nome de quem de fato realizou o pagamento físico (Nome na GRU).
     */
    @Column(name = "contributor_name", length = 45)
    private String contributorName;

    /**
     * Validade em horas do QR Code do Pix (1 a 23 horas).
     */
    @Column(name = "pix_expiration_hours")
    private Integer pixExpirationHours;

    /**
     * URL retornada pelo PagTesouro para redirecionamento do contribuinte.
     */
    @Column(name = "next_url", length = 2048)
    private String nextUrl;

    /**
     * Controle de auditoria: data e hora em que a solicitação de cobrança foi gerada.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Controle de auditoria: data da compensação bancária e liquidação do pagamento.
     */
    @Column(name = "paid_at")
    private Instant paidAt;

    /**
     * Relacionamento Muitos-para-Muitos: Associa quais itens do pedido estão vinculados
     * e serão quitados por esta tentativa de pagamento.
     */
    @ManyToMany
    @JoinTable(
        name = "payment_items",
        joinColumns = @JoinColumn(name = "payment_id"),
        inverseJoinColumns = @JoinColumn(name = "order_item_id")
    )
    private List<OrderItem> orderItems;
}
