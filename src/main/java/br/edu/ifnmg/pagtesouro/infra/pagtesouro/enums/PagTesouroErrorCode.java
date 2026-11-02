package br.edu.ifnmg.pagtesouro.infra.pagtesouro.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Catálogo dos códigos oficiais de erro e rejeição da API do PagTesouro (STN).
 *
 * @author Caio da Silva Viana
 */
@Getter
public enum PagTesouroErrorCode {

    C0001("C0001", "Parâmetros obrigatórios ausentes na solicitação ao PagTesouro.", HttpStatus.UNPROCESSABLE_CONTENT),
    C0012("C0012", "Código de serviço SISGRU inválido ou inativo na Secretaria do Tesouro Nacional.", HttpStatus.UNPROCESSABLE_CONTENT),
    C0015("C0015", "Valor do pagamento divergente ou incompatível com o serviço configurado na STN.", HttpStatus.UNPROCESSABLE_CONTENT),
    C0024("C0024", "URL de notificação inválida: a STN exige domínio oficial governamental (.gov.br) ou educacional (.edu.br).", HttpStatus.UNPROCESSABLE_CONTENT),
    AUTH_ERROR("AUTH_ERROR", "Falha de autenticação com o PagTesouro. O Token de integração é inválido ou expirou.", HttpStatus.BAD_GATEWAY),
    GATEWAY_ERROR("GATEWAY_ERROR", "A API do PagTesouro da STN está temporariamente indisponível.", HttpStatus.BAD_GATEWAY),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Erro não catalogado retornado pelo gateway do PagTesouro.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String userFriendlyMessage;
    private final HttpStatus httpStatus;

    PagTesouroErrorCode(String code, String userFriendlyMessage, HttpStatus httpStatus) {
        this.code = code;
        this.userFriendlyMessage = userFriendlyMessage;
        this.httpStatus = httpStatus;
    }

    public static PagTesouroErrorCode fromCode(String code) {
        if (code == null) return UNKNOWN_ERROR;
        for (PagTesouroErrorCode error : values()) {
            if (error.getCode().equalsIgnoreCase(code.trim())) {
                return error;
            }
        }
        return UNKNOWN_ERROR;
    }
}
