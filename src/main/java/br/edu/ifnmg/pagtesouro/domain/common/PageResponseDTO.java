package br.edu.ifnmg.pagtesouro.domain.common;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Objeto de Transferência de Dados (DTO) genérico e limpo para respostas paginadas da API REST.
 * <p>
 * Elimina o excesso de boilerplate, metadados internos de ordenação e estruturas aninhadas
 * desnecessárias do {@link Page} padrão do Spring Data JPA, devolvendo exatamente os atributos
 * essenciais para a montagem de tabelas e paginações fluidas no front-end.
 * </p>
 *
 * @param <T> O tipo dos dados contidos na lista
 * @param content Lista com os registros de dados retornados nesta página
 * @param page Índice da página atual (base 0)
 * @param totalPages Quantidade total de páginas disponíveis
 * @param totalElements Quantidade total de registros encontrados no banco de dados
 * @param first Indicador booleano se esta é a primeira página
 * @param last Indicador booleano se esta é a última página
 *
 * @author Caio da Silva Viana
 */
public record PageResponseDTO<T>(
    List<T> content,
    int page,
    int totalPages,
    long totalElements,
    boolean first,
    boolean last
) {
    /**
     * Construtor auxiliar que mapeia diretamente uma instância de {@link Page} do Spring Data.
     *
     * @param page A página original retornada pelo repositório ou serviço.
     */
    public PageResponseDTO(Page<T> page) {
        this(
            page.getContent(),
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.isFirst(),
            page.isLast()
        );
    }
}
