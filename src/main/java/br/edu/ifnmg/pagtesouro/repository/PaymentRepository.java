package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link Payment}.
 * <p>
 * **Conceito no TCC:**
 * Centraliza o acesso e auditoria das tentativas de transações enviadas à Secretaria do Tesouro Nacional (STN).
 * Oferece métodos essenciais de busca por ID de rastreamento do PagTesouro e para varreduras failsafe.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Localiza um pagamento no banco local pelo ID de solicitação externo retornado pelo PagTesouro.
     * Utilizado para localizar a cobrança no momento do processamento assíncrono de notificações de webhook.
     * Utiliza {@link EntityGraph} para carregar ansiosamente a coleção associada de {@link br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem},
     * prevenindo {@code LazyInitializationException} em threads reativas e assíncronas do webhook.
     *
     * @param pagtesouroPaymentId O ID do pagamento retornado na solicitação do PagTesouro
     * @return Um {@link Optional} contendo o pagamento com itens carregados se localizado; vazio caso contrário
     */
    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Payment> findByPagtesouroPaymentId(String pagtesouroPaymentId);

    /**
     * Localiza todos os pagamentos pendentes no banco com base em seu status e que foram criados antes
     * de uma determinada data limite. Utilizado pelo daemon de hora em hora para verificar faturas perdidas.
     *
     * @param status O status do pagamento a filtrar (normalmente {@link PaymentStatus#CREATED})
     * @param createdAt A data e hora limite máxima de criação do pagamento
     * @return Uma lista de pagamentos correspondentes localizados
     */
    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdAt);

    /**
     * Localiza e pagina o histórico de tentativas de pagamento efetuadas por um contribuinte específico.
     * Utilizado para alimentar a tela de histórico financeiro do estudante logado no portal.
     *
     * @param contributorCpfCnpj O CPF/CNPJ de quem realizou a solicitação de guia
     * @param pageable Configuração de paginação (número de página, tamanho e ordenação)
     * @return Uma página contendo os pagamentos correspondentes localizados
     */
    Page<Payment> findByContributorCpfCnpj(String contributorCpfCnpj, Pageable pageable);
}
