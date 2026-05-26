package br.edu.ifnmg.pagtesouro.infra.exception;

import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Interceptador Global de Exceções REST (Controller Advice) da aplicação.
 * <p>
 * **Conceito no TCC (Robustez e Padronização de Erros):**
 * Captura de forma centralizada qualquer exceção lançada na camada de controladores, serviços ou
 * segurança e a converte em um payload HTTP padronizado e limpo, evitando o vazamento de detalhes
 * internos de compilação ou de banco de dados (Stacktraces) para o usuário final, aumentando a segurança.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Trata falhas de autenticação e credenciais inválidas.
   * Retorna status 401 Unauthorized.
   */
  @ExceptionHandler(AuthenticationException.class)
  private @NonNull ResponseEntity<RestErrorMessage> authenticationException() {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.UNAUTHORIZED, "E-mail ou senha informados são inválidos!");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(restErrorMessage);
  }

  /**
   * Trata violações lógicas de unicidade e conflitos de dados.
   * Retorna status 409 Conflict.
   */
  @ExceptionHandler(ConflictException.class)
  private @NonNull ResponseEntity<RestErrorMessage> conflictException(ConflictException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.CONFLICT, e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(restErrorMessage);
  }

  /**
   * Trata consultas de recursos não localizados nas tabelas.
   * Retorna status 400 Bad Request.
   */
  @ExceptionHandler(FindException.class)
  private @NonNull ResponseEntity<RestErrorMessage> findException(FindException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restErrorMessage);
  }

  /**
   * Trata falhas de argumentos e parâmetros de entrada logicamente incorretos.
   * Retorna status 400 Bad Request.
   *
   * @param e A exceção de argumento inválido lançada.
   * @return Payload de erro estruturado.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  private @NonNull ResponseEntity<RestErrorMessage> handleIllegalArgument(IllegalArgumentException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restErrorMessage);
  }

  /**
   * Trata violações de estado de ciclo de vida de negócios da aplicação.
   * <p>
   * **Cenário no TCC:**
   * Amplamente acionado no fluxo de quitações e baixas físicas, impedindo que o sistema processe
   * consumos de tíquetes não pagos ou que efetue novas baixas em itens já trocados anteriormente (gasto duplo).
   * Retorna status 422 Unprocessable Entity (Unprocessable Content).
   * </p>
   *
   * @param e A exceção de estado ilegal lançada.
   * @return Payload estruturado com a mensagem lógica de barreira de negócios.
   */
  @ExceptionHandler(IllegalStateException.class)
  private @NonNull ResponseEntity<RestErrorMessage> handleIllegalState(IllegalStateException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(restErrorMessage);
  }

  /**
   * Trata violações de segurança e restrições de permissões de acesso da API.
   * <p>
   * **Cenário no TCC:**
   * Captura tentativas de alunos acessando faturas de terceiros ou usuários comuns tentando
   * acionar o guichê de baixas de tíquetes sem possuir privilégio formal de administrador (ADMIN).
   * Retorna status 403 Forbidden.
   * </p>
   *
   * @param e A exceção de violação de segurança lançada.
   * @return Payload estruturado informando o bloqueio de segurança.
   */
  @ExceptionHandler(SecurityException.class)
  private @NonNull ResponseEntity<RestErrorMessage> handleSecurityException(SecurityException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.FORBIDDEN, e.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(restErrorMessage);
  }

  /**
   * Trata erros operacionais não tratados de runtime de forma genérica.
   * Retorna status 500 Internal Server Error.
   */
  @ExceptionHandler(RuntimeException.class)
  private @NonNull ResponseEntity<RestErrorMessage> runtimeException(RuntimeException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(restErrorMessage);
  }

  /**
   * Trata exceções genéricas de infraestrutura não mapeadas.
   * Retorna status 500 Internal Server Error.
   */
  @ExceptionHandler(Exception.class)
  private @NonNull ResponseEntity<RestErrorMessage> exception(Exception e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(restErrorMessage);
  }
}
