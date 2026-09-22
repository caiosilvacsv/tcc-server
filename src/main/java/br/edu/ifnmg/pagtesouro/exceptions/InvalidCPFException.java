package br.edu.ifnmg.pagtesouro.exceptions;

/**
 * Exceção personalizada disparada quando a validação do CPF falha matematicamente.
 * <p>
 * **Cenário no TCC:**
 * Lançada no fluxo de cadastro de usuários e checagem de dados cadastrais caso o CPF informado
 * não passe nos cálculos dos dígitos verificadores executados pelo validador de CPF local.
 * </p>
 *
 * @author Caio da Silva Viana
 */
public class InvalidCPFException extends RuntimeException {
  public InvalidCPFException() {
    super("CPF informado é matematicamente inválido!");
  }
}
