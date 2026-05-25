package br.edu.ifnmg.pagtesouro.exceptions;

/**
 * Exceção personalizada disparada quando ocorre um conflito lógico de persistência de dados.
 * <p>
 * **Cenário no TCC:**
 * Utilizada principalmente no fluxo de registro de usuários para indicar que a credencial
 * única informada (como um CPF ou E-mail que já existe fisicamente no banco) viola as restrições
 * de unicidade lógica do banco de dados PostgreSQL.
 * </p>
 *
 * @author Caio da Silva Viana
 */
public class ConflictException extends RuntimeException {
  
  public ConflictException(String message) {
    super(message);
  }
  
  public ConflictException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public ConflictException(Throwable cause) {
    super(cause);
  }
  
  public ConflictException(){
    super("Erro: Conflito de dados detectado!");
  }
}
