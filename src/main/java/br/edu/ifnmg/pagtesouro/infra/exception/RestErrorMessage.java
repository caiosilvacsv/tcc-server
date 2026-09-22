package br.edu.ifnmg.pagtesouro.infra.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * Estrutura de DTO padronizada para retorno de falhas e erros em requisições REST da API.
 * <p>
 * **Conceito no TCC:**
 * Modela a mensagem de erro que é retornada de forma legível e padronizada para o frontend,
 * contendo o status HTTP lógico e uma mensagem de esclarecimento didática sobre o erro.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@AllArgsConstructor
@Getter
@Setter
public class RestErrorMessage {
    /**
     * O status HTTP de retorno (ex: 400 Bad Request, 401 Unauthorized, 409 Conflict).
     */
    private HttpStatus status;
    
    /**
     * Descrição simplificada ou detalhada da falha lógica que provocou o erro.
     */
    private String message;
}
