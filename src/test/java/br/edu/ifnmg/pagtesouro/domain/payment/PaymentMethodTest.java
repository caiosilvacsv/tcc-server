package br.edu.ifnmg.pagtesouro.domain.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Meio de Pagamento (PaymentMethod)")
class PaymentMethodTest {

    @Test
    @DisplayName("Deve converter string correspondente para o Enum PaymentMethod de forma segura")
    void fromMethod_Success() {
        assertEquals(PaymentMethod.PIX, PaymentMethod.fromMethod("PIX"));
        assertEquals(PaymentMethod.CREDIT_CARD, PaymentMethod.fromMethod("CARTAO_CREDITO"));
        assertEquals(PaymentMethod.WALLET_BALANCE, PaymentMethod.fromMethod("SALDO_CARTEIRA"));
        assertEquals(PaymentMethod.GRU, PaymentMethod.fromMethod("BOLETO"));
        
        // Com letras minúsculas, espaços e nome do enum
        assertEquals(PaymentMethod.PIX, PaymentMethod.fromMethod("  pix  "));
        assertEquals(PaymentMethod.GRU, PaymentMethod.fromMethod("boleto"));
        assertEquals(PaymentMethod.GRU, PaymentMethod.fromMethod("GRU"));
    }

    @Test
    @DisplayName("Deve lançar exceção para valores nulos ou inválidos")
    void fromMethod_Exceptions() {
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromMethod(null));
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromMethod("invalido"));
        assertThrows(IllegalArgumentException.class, () -> PaymentMethod.fromMethod(""));
    }
}
