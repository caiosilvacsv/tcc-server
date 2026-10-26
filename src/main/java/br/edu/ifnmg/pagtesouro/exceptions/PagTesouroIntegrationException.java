package br.edu.ifnmg.pagtesouro.exceptions;

import br.edu.ifnmg.pagtesouro.infra.pagtesouro.enums.PagTesouroErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exceção semântica para falhas de integração e rejeições de negócios da API do PagTesouro (STN).
 *
 * @author Caio da Silva Viana
 */
@Getter
public class PagTesouroIntegrationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String details;

    public PagTesouroIntegrationException(PagTesouroErrorCode errorCode, String details) {
        super(errorCode.getUserFriendlyMessage());
        this.errorCode = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
        this.details = details;
    }

    public PagTesouroIntegrationException(String message, HttpStatus httpStatus, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details;
    }
}
