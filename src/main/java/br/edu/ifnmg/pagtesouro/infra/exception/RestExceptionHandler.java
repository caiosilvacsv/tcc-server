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

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(AuthenticationException.class)
  private @NonNull ResponseEntity<RestErrorMessage> authenticationException() {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.UNAUTHORIZED, "Email or password invalid");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(restErrorMessage);
  }

  @ExceptionHandler(ConflictException.class)
  private @NonNull ResponseEntity<RestErrorMessage> conflictException( ConflictException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.CONFLICT, e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(restErrorMessage);
  }

  @ExceptionHandler(FindException.class)
  private @NonNull ResponseEntity<RestErrorMessage> findException( FindException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.BAD_REQUEST, e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(restErrorMessage);
  }

  @ExceptionHandler(RuntimeException.class)
  private @NonNull ResponseEntity<RestErrorMessage> runtimeException(RuntimeException e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(restErrorMessage);
  }

  @ExceptionHandler(Exception.class)
  private @NonNull ResponseEntity<RestErrorMessage> exception(Exception e) {
    RestErrorMessage restErrorMessage = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(restErrorMessage);
  }
}
