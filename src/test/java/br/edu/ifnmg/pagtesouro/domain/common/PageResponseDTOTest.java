package br.edu.ifnmg.pagtesouro.domain.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do DTO de Paginação Limpa (PageResponseDTO)")
class PageResponseDTOTest {

    @Test
    @DisplayName("Deve converter um Page do Spring em PageResponseDTO com os 5 elementos essenciais")
    void shouldConvertSpringPageToPageResponseDTO() {
        List<String> items = List.of("Item 1", "Item 2", "Item 3");
        Page<String> springPage = new PageImpl<>(items, PageRequest.of(0, 10), 25);

        PageResponseDTO<String> dto = new PageResponseDTO<>(springPage);

        assertNotNull(dto);
        assertEquals(items, dto.content());
        assertEquals(0, dto.page());
        assertEquals(3, dto.totalPages());
        assertEquals(25, dto.totalElements());
        assertTrue(dto.first());
        assertFalse(dto.last());
    }
}
