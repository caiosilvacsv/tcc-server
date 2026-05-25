package br.edu.ifnmg.pagtesouro.domain.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA que representa um item cobrável (Produto ou Serviço) comercializado no sistema.
 * <p>
 * **Conceito no TCC:**
 * Para manter a flexibilidade e suportar qualquer tipo de arrecadação do IFNMG,
 * taxas de inscrição em processos seletivos, cobranças de vestibular, tíquetes de alimentação,
 * taxas de emissão de documentos e demais receitas da instituição são modeladas genericamente sob a classe
 * {@code Product}.
 * </p>
 * <p>
 * Cada produto está associado a um código de serviço do SISGRU (sistema de controle da arrecadação federal),
 * permitindo que faturas específicas sejam enviadas e integradas diretamente com a API do PagTesouro da STN.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products")
@Entity(name = "products")
public class Product {

    /**
     * Identificador único do produto no banco de dados.
     * Utiliza geração de chaves primárias do tipo {@code UUID} para maior segurança e portabilidade.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Título ou nome comercial do produto/serviço.
     * <p>Exemplo: "Ticket Refeição - Restaurante Estudantil"</p>
     */
    @NotNull
    @Column(nullable = false)
    private String title;

    /**
     * Detalhes ou descrição sobre as regras do produto/serviço.
     */
    private String description;

    /**
     * Link ou caminho relativo para a imagem do produto.
     */
    private String image;

    /**
     * Preço unitário do produto.
     * <p>
     * Mapeado no Java como {@link BigDecimal} para cálculos exatos e precisão absoluta.
     * No banco de dados, é convertido e persistido como centavos (inteiro de 32 bits) via
     * {@link MoneyToCentsConverter} para garantir indexação rápida e evitar erros de arredondamento.
     * </p>
     */
    @NotNull
    @Column(nullable = false)
    @Convert(converter = MoneyToCentsConverter.class)
    private BigDecimal price;

    /**
     * Status de ativação do produto.
     * Se falso, o produto fica invisível para novas vendas e compras no frontend.
     */
    private boolean active;

    /**
     * Código de serviço único registrado no SISGRU correspondente a esta receita.
     * <p>
     * **Importância:** Este campo é obrigatório para realizar a chamada {@code POST} de solicitação
     * de pagamentos junto à API do PagTesouro.
     * </p>
     * <p>Exemplo: "028031" (Tíquete Lanche)</p>
     */
    @NotNull
    @Column(nullable = false, name = "code_service")
    private String codeService;

    /**
     * Categoria de enquadramento do produto/serviço.
     * Mapeado como String legível no banco de dados.
     *
     * @see ProductCategory
     */
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    /**
     * Controle de auditoria: Registra automaticamente a data e hora em que o produto foi cadastrado.
     * Definido no momento do {@code INSERT} físico e marcado como não atualizável.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Controle de auditoria: Atualiza automaticamente a data e hora sempre que o registro for alterado.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
