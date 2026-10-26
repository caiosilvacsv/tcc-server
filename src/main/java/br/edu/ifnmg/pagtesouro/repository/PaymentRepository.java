package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de repositório JPA responsável pelas operações de persistência da entidade {@link Payment}.
 *
 * @author Caio da Silva Viana
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Payment> findByPagtesouroPaymentId(String pagtesouroPaymentId);

    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdAt);

    Page<Payment> findAllByContributorCpfCnpj(String contributorCpfCnpj, Pageable pageable);

    Page<Payment> findByContributorCpfCnpj(String contributorCpfCnpj, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus.COMPLETED")
    BigDecimal sumTotalConfirmedRevenue();

    @Query("""
        SELECT DISTINCT p FROM Payment p
        JOIN p.orderItems item
        WHERE item.order.id = :orderId
          AND p.status IN (br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus.CREATED, br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus.SUBMITTED)
    """)
    List<Payment> findPendingPaymentsByOrderId(@org.springframework.data.repository.query.Param("orderId") UUID orderId);
}
