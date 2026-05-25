package br.edu.ifnmg.pagtesouro.exceptions;

/**
 * Exceção personalizada disparada quando uma busca de recurso não retorna nenhum registro.
 * <p>
 * **Cenário no TCC:**
 * Utilizada extensivamente nos serviços para indicar que uma entidade essencial pesquisada pelo
 * ID único (como um Produto, Pedido ou Pagamento) não foi localizada fisicamente nas tabelas do banco.
 * </p>
 *
 * @author Caio da Silva Viana
 */
public class FindException extends RuntimeException {
  public FindException(String message) {
    super(message);
  }
}
