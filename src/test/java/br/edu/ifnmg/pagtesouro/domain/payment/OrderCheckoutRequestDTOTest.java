package br.edu.ifnmg.pagtesouro.domain.payment;

import br.edu.ifnmg.pagtesouro.domain.payment.dto.OrderCheckoutRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderCheckoutRequestDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testDeserializeEmptyJson() throws Exception {
        OrderCheckoutRequestDTO dto = objectMapper.readValue("{}", OrderCheckoutRequestDTO.class);
        assertNotNull(dto);
        assertNull(dto.contributorCpfCnpj());
        assertNull(dto.contributorName());
        assertFalse(dto.isMobile());
    }

    @Test
    void testDeserializeFullJson() throws Exception {
        String json = "{\"contributorCpfCnpj\":\"12345678909\",\"contributorName\":\"Caio\",\"isMobile\":true}";
        OrderCheckoutRequestDTO dto = objectMapper.readValue(json, OrderCheckoutRequestDTO.class);
        assertNotNull(dto);
        assertEquals("12345678909", dto.contributorCpfCnpj());
        assertEquals("Caio", dto.contributorName());
        assertTrue(dto.isMobile());
    }
}
