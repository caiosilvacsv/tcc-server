package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Payment> findByPagtesouroPaymentId(String pagtesouroPaymentId);

    List<Payment> findAllByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdAt);

    Page<Payment> findAllByContributorCpfCnpj(String contributorCpfCnpj, Pageable pageable);

    Page<Payment> findByContributorCpfCnpj(String contributorCpfCnpj, Pageable pageable);

    @Query("SELECT DISTINCT p FROM payments p JOIN p.orderItems oi JOIN oi.order o WHERE o.user.id = :userId")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<Payment> findAllByStatus(PaymentStatus status);

    default BigDecimal sumTotalConfirmedRevenue() {
        return findAllByStatus(PaymentStatus.COMPLETED).stream()
                .map(Payment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Query("""
        SELECT DISTINCT p FROM payments p
        JOIN p.orderItems item
        WHERE item.order.id = :orderId
          AND p.status IN (br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus.CREATED, br.edu.ifnmg.pagtesouro.domain.payment.PaymentStatus.SUBMITTED)
    """)
    List<Payment> findPendingPaymentsByOrderId(@org.springframework.data.repository.query.Param("orderId") UUID orderId);
}
