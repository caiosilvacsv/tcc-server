package br.edu.ifnmg.pagtesouro.infra.exception;

import br.edu.ifnmg.pagtesouro.exceptions.ExceptionExemple;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ExceptionExemple.class)
  private @NonNull ResponseEntity<RestErrorMessage> exceptionTest(ExceptionExemple e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.NOT_FOUND, e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(restErrorMessage);

  }

  @ExceptionHandler(AuthenticationException.class)
  private @NonNull ResponseEntity<RestErrorMessage> authenticationException() {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.UNAUTHORIZED, "Login ou senha inválidos");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(restErrorMessage);
  }

  @ExceptionHandler(RuntimeException.class)
  private @NonNull ResponseEntity<RestErrorMessage> runtimeException(RuntimeException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(restErrorMessage);
  }


}
