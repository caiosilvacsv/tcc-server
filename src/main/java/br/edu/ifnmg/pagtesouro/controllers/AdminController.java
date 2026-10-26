package br.edu.ifnmg.pagtesouro.controllers;

import br.edu.ifnmg.pagtesouro.domain.admin.dto.AdminDashboardResponseDTO;
import br.edu.ifnmg.pagtesouro.domain.payment.Payment;
import br.edu.ifnmg.pagtesouro.domain.payment.dto.CheckoutResponseDTO;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import br.edu.ifnmg.pagtesouro.repository.PaymentRepository;
import br.edu.ifnmg.pagtesouro.services.admin.AdminDashboardService;
import br.edu.ifnmg.pagtesouro.services.payment.PaymentSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

/**
 * Controlador REST para rotas administrativas (dashboard e conciliação forçada no balcão).
 *
 * @author Caio da Silva Viana
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final PaymentRepository paymentRepository;
    private final PaymentSyncService paymentSyncService;

    public AdminController(
            AdminDashboardService dashboardService,
            PaymentRepository paymentRepository,
            PaymentSyncService paymentSyncService) {
        this.dashboardService = dashboardService;
        this.paymentRepository = paymentRepository;
        this.paymentSyncService = paymentSyncService;
    }

    /**
     * Retorna o dashboard analítico com as métricas consolidadas da arrecadação e tíquetes.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponseDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardMetrics());
    }

    /**
     * Força a sincronização imediata de uma fatura contra a API da STN (atendimento ágil no guichê).
     */
    @PostMapping("/payments/{paymentId}/sync")
    public ResponseEntity<CheckoutResponseDTO> forceSyncPayment(@PathVariable UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new FindException("Fatura de pagamento não encontrada com ID: " + paymentId));

        if (payment.getPagtesouroPaymentId() == null || payment.getPagtesouroPaymentId().isBlank()) {
            throw new IllegalStateException("Esta fatura ainda não possui ID de solicitação registrado junto ao PagTesouro.");
        }

        Payment synced = paymentSyncService.syncPaymentStatus(payment.getPagtesouroPaymentId())
            .block(Duration.ofSeconds(12));

        if (synced == null) {
            synced = payment;
        }

        CheckoutResponseDTO response = new CheckoutResponseDTO(
            synced.getId(),
            synced.getPagtesouroPaymentId(),
            synced.getAmount(),
            synced.getStatus().name(),
            synced.getNextUrl()
        );

        return ResponseEntity.ok(response);
    }
}
