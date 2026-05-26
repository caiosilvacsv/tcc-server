package br.edu.ifnmg.pagtesouro.domain.payment;

import br.edu.ifnmg.pagtesouro.domain.orderItem.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da Entidade de Pagamento (Payment)")
class PaymentTest {

    @Test
    @DisplayName("Deve inicializar os desmembramentos de valores zerados por padrão")
    void defaultAmounts() {
        Payment payment = new Payment();
        
        assertEquals(BigDecimal.ZERO, payment.getPrincipalAmount());
        assertEquals(BigDecimal.ZERO, payment.getDiscountAmount());
        assertEquals(BigDecimal.ZERO, payment.getDeductionsAmount());
        assertEquals(BigDecimal.ZERO, payment.getFineAmount());
        assertEquals(BigDecimal.ZERO, payment.getInterestAmount());
        assertEquals(BigDecimal.ZERO, payment.getAdditionsAmount());
    }

    @Test
    @DisplayName("Deve validar a modelagem e atribuições completas do Pagamento")
    void paymentProperties() {
        Payment payment = new Payment();
        UUID id = UUID.randomUUID();
        List<OrderItem> items = new ArrayList<>();
        
        payment.setId(id);
        payment.setPagtesouroPaymentId("solic_12345");
        payment.setReference("REF99");
        payment.setCompetence("202605");
        payment.setExpiredAt(LocalDate.of(2026, 5, 23));
        payment.setAmount(new BigDecimal("15.50"));
        payment.setStatus(PaymentStatus.CREATED);
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setPspName("Simulador PSP");
        payment.setPspTransactionId("trans_9999");
        
        payment.setPrincipalAmount(new BigDecimal("10.00"));
        payment.setDiscountAmount(new BigDecimal("1.00"));
        payment.setDeductionsAmount(new BigDecimal("0.50"));
        payment.setFineAmount(new BigDecimal("3.00"));
        payment.setInterestAmount(new BigDecimal("2.00"));
        payment.setAdditionsAmount(new BigDecimal("1.00"));
        
        payment.setContributorCpfCnpj("11144477735");
        payment.setContributorName("Caio Silva Viana");
        payment.setPixExpirationHours(12);
        payment.setNextUrl("https://valpagtesouro.gov.br/simulador/iframe");
        payment.setOrderItems(items);
        
        assertEquals(id, payment.getId());
        assertEquals("solic_12345", payment.getPagtesouroPaymentId());
        assertEquals("REF99", payment.getReference());
        assertEquals("202605", payment.getCompetence());
        assertEquals(LocalDate.of(2026, 5, 23), payment.getExpiredAt());
        assertEquals(new BigDecimal("15.50"), payment.getAmount());
        assertEquals(PaymentStatus.CREATED, payment.getStatus());
        assertEquals(PaymentMethod.PIX, payment.getPaymentMethod());
        assertEquals("Simulador PSP", payment.getPspName());
        assertEquals("trans_9999", payment.getPspTransactionId());
        
        assertEquals(new BigDecimal("10.00"), payment.getPrincipalAmount());
        assertEquals(new BigDecimal("1.00"), payment.getDiscountAmount());
        assertEquals(new BigDecimal("0.50"), payment.getDeductionsAmount());
        assertEquals(new BigDecimal("3.00"), payment.getFineAmount());
        assertEquals(new BigDecimal("2.00"), payment.getInterestAmount());
        assertEquals(new BigDecimal("1.00"), payment.getAdditionsAmount());
        
        assertEquals("11144477735", payment.getContributorCpfCnpj());
        assertEquals("Caio Silva Viana", payment.getContributorName());
        assertEquals(12, payment.getPixExpirationHours());
        assertEquals("https://valpagtesouro.gov.br/simulador/iframe", payment.getNextUrl());
        assertSame(items, payment.getOrderItems());
    }
}
