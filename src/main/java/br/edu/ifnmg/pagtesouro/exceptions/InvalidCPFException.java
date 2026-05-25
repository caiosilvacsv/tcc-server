package br.edu.ifnmg.pagtesouro.exceptions;

/**
 * Exception thrown when a CPF value is invalid.
 */
public class InvalidCPFException extends RuntimeException {
  public InvalidCPFException() {
    super("Invalid CPF");
  }
}

