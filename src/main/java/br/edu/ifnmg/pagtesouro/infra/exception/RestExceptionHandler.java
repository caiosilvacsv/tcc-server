package br.edu.ifnmg.pagtesouro.infra.exception;

import br.edu.ifnmg.pagtesouro.exceptions.ConflictException;
import br.edu.ifnmg.pagtesouro.exceptions.FindException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Trata erros de recursos não encontradas.
   * <p>
   * **Cenário no TCC:**
   * Captura tentativas de acessar rotas inexistente para aquela rota.
   * Retorna status 404 NOT FOUND.
   * </p>
   *
   * @param ex A exceção de violação de segurança lançada.
   * @param headers O header enviado pelo usyário.
   * @param status Status da requisição.
   * @param request A requisição enviada pelo usuário.
   * @return Payload estruturado informando a rota não encontrada.
   */
  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    RestErrorMessage errorMessage = new RestErrorMessage(
        HttpStatus.NOT_FOUND,
        "A rota solicitada '" + ex.getResourcePath() + "' não foi encontrada nesta API."
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorMessage);
  }

  /**
   * Trata incompatibilidade de cabeçalhos Accept / Content-Type da requisição.
   * Evita falhas internas no serializador quando um endpoint específico (como SSE) lança exceção.
   */
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    RestErrorMessage errorMessage = new RestErrorMessage(
        HttpStatus.NOT_ACCEPTABLE,
        "O formato de mídia requisitado não é suportado para esta resposta."
    );
    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorMessage);
  }

  /**
   * Trata falhas de autenticação e credenciais inválidas.
   * Return Status 401 Unauthorized.
   * @return Payload com mensagem adequada.
   */
  @ExceptionHandler(AuthenticationException.class)
  public @NonNull ResponseEntity<RestErrorMessage> authenticationException() {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.UNAUTHORIZED, "E-mail ou senha informados são inválidos!");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
  }

  /**
   * Trata violações lógicas de unicidade e conflitos de dados.
   * Retorna status 409 Conflict.
   * @param e Exceção criada.
   * @return Payload estruturado com mensagem de erro.
   */
  @ExceptionHandler(ConflictException.class)
  public @NonNull ResponseEntity<RestErrorMessage> conflictException(ConflictException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.CONFLICT, e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
  }

  /**
   * Trata consultas de recursos não localizados nas tabelas.
   * Retorna status 400 Bad Request.
   */
  @ExceptionHandler(FindException.class)
  public @NonNull ResponseEntity<RestErrorMessage> findException(FindException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
  }

  /**
   * Trata falhas de argumentos e parâmetros de entrada logicamente incorretos.
   * Retorna status 400 Bad Request.
   *
   * @param e A exceção de argumento inválido lançada.
   * @return Payload de erro estruturado.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public @NonNull ResponseEntity<RestErrorMessage> handleIllegalArgument(IllegalArgumentException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
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
  public @NonNull ResponseEntity<RestErrorMessage> handleIllegalState(IllegalStateException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
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
  public @NonNull ResponseEntity<RestErrorMessage> handleSecurityException(SecurityException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.FORBIDDEN, e.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
  }

  /**
   * Trata erros operacionais não tratados de runtime de forma genérica.
   * Retorna status 500 Internal Server Error.
   */
  @ExceptionHandler(RuntimeException.class)
  public @NonNull ResponseEntity<RestErrorMessage> runtimeException(RuntimeException e) {
    String message = e.getMessage();
    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
        org.springframework.web.reactive.function.client.WebClientResponseException wcre =
            (org.springframework.web.reactive.function.client.WebClientResponseException) e;
        message += " - Response Body: " + wcre.getResponseBodyAsString();
    }
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, message);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
  }

  /**
   * Trata exceções genéricas de infraestrutura não mapeadas.
   * Retorna status 500 Internal Server Error.
   */
  @ExceptionHandler(Exception.class)
  public @NonNull ResponseEntity<RestErrorMessage> exception(Exception e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(restErrorMessage);
  }
}
