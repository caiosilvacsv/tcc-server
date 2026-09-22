package br.edu.ifnmg.pagtesouro.domain.user;

/**
 * Classe utilitária responsável pela normalização e validação matemática de CPFs (Cadastro de Pessoas Físicas).
 * <p>
 * **Funcionamento:**
 * O validador aceita CPFs formatados (com pontos e traço) ou apenas dígitos. Antes de processar a validação,
 * todos os caracteres não numéricos são removidos, permitindo entradas flexíveis como {@code 123.456.789-09}
 * ou {@code 12345678909}.
 * </p>
 *
 * @author Caio da Silva Viana
 */
public final class CpfValidator {
  private CpfValidator(){}

  /**
   * Remove todos os caracteres não numéricos de uma string de CPF, deixando apenas os dígitos.
   *
   * @param cpf O CPF a ser normalizado, com ou sem pontuação
   * @return Uma string contendo apenas os 11 dígitos numéricos do CPF, ou {@code null} se a entrada for nula
   */
  public static String normalize(String cpf) {
    if (cpf == null) {
      return null;
    }

    return cpf.replaceAll("\\D", "");
  }

  /**
   * Valida matematicamente um CPF brasileiro por meio do cálculo de seus dígitos verificadores.
   * <p>
   * **Critérios de Validação:**
   * <ul>
   *   <li>O CPF deve possuir exatamente 11 dígitos numéricos após a normalização.</li>
   *   <li>São rejeitados CPFs com todos os dígitos repetidos (ex: {@code 00000000000}, {@code 11111111111}),
   *       pois estes passam no teste matemático dos dígitos verificadores, mas são CPFs inválidos no cadastro da Receita Federal.</li>
   *   <li>Realiza o cálculo ponderado dos dois dígitos verificadores finais (dígito 9 e dígito 10) e compara com os fornecidos.</li>
   * </ul>
   * </p>
   *
   * @param cpf O CPF a ser validado, formatado ou contendo apenas dígitos
   * @return {@code true} se o CPF for considerado matematicamente válido; {@code false} caso contrário
   */
  public static boolean isValid(String cpf) {
    String normalizedCpf = normalize(cpf);

    if (normalizedCpf == null || !normalizedCpf.matches("\\d{11}")) {
      return false;
    }

    if (normalizedCpf.chars().distinct().count() == 1) {
      return false;
    }

    int firstDigit = calculateDigit(normalizedCpf, 9, 10);
    int secondDigit = calculateDigit(normalizedCpf, 10, 11);

    return firstDigit == Character.getNumericValue(normalizedCpf.charAt(9))
        && secondDigit == Character.getNumericValue(normalizedCpf.charAt(10));
  }

  /**
   * Calcula um dígito verificador específico de CPF usando a soma ponderada de seus caracteres numéricos.
   * <p>
   * **Algoritmo de Cálculo:**
   * Cada um dos dígitos anteriores é multiplicado por pesos decrescentes (iniciando no peso máximo e decrescendo de 1 em 1).
   * A soma total destas multiplicações é dividida por 11, e o resto da divisão é utilizado para definir o dígito:
   * <ul>
   *   <li>Se o resto da divisão for menor que 2, o dígito verificador é {@code 0}.</li>
   *   <li>Caso contrário, o dígito verificador é {@code 11 - resto}.</li>
   * </ul>
   * </p>
   *
   * @param cpf O CPF normalizado contendo apenas dígitos
   * @param length A quantidade de dígitos iniciais do CPF a serem utilizados no cálculo (9 para o 1º dígito, 10 para o 2º)
   * @param weight O peso inicial para o cálculo ponderado (10 para o 1º dígito, 11 para o 2º)
   * @return O dígito verificador calculado (valor entre 0 e 9)
   */
  private static int calculateDigit(String cpf, int length, int weight) {
    int sum = 0;

    for (int i = 0; i < length; i++) {
      sum += Character.getNumericValue(cpf.charAt(i)) * (weight - i);
    }

    int result = 11 - (sum % 11);
    return result >= 10 ? 0 : result;
  }
}


