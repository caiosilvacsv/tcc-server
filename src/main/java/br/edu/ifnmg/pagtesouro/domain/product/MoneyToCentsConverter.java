package br.edu.ifnmg.pagtesouro.domain.product;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversor JPA responsável por realizar a ponte de persistência entre valores monetários.
 * <p>
 * **Problema resolvido:**
 * O uso de tipos flutuantes primitivos (como {@code float} ou {@code double}) ou decimais complexos
 * em bancos de dados relacionais pode ocasionar problemas graves de arredondamento aritmético ou
 * lentidão em consultas indexadas.
 * </p>
 * <p>
 * **Solução Adotada (Padrão de Mercado):**
 * Os valores monetários são representados em Java como {@link BigDecimal} para garantir precisão absoluta
 * nas regras de negócio e operações matemáticas. No banco de dados PostgreSQL, os valores são armazenados
 * como **centavos** em colunas do tipo {@code INTEGER} (inteiros de 32 bits), o que garante alta performance,
 * integridade física absoluta e armazenamento compacto.
 * </p>
 * <p>
 * **Exemplo de Conversão:**
 * <ul>
 *   <li>Java: {@code BigDecimal("2.50")} (R$ 2,50) &rarr; Banco: {@code 250} (centavos)</li>
 *   <li>Java: {@code BigDecimal("15.00")} (R$ 15,00) &rarr; Banco: {@code 1500} (centavos)</li>
 *   <li>Java: {@code BigDecimal("0.75")} (R$ 0,75) &rarr; Banco: {@code 75} (centavos)</li>
 * </ul>
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Converter
public class MoneyToCentsConverter implements AttributeConverter<BigDecimal, Integer> {

    /**
     * Converte o valor monetário decimal de {@link BigDecimal} para {@link Integer} (centavos)
     * no momento de salvar o registro no banco de dados (INSERT/UPDATE).
     *
     * @param value O valor em formato {@link BigDecimal} (ex: 2.50)
     * @return O valor em centavos como {@link Integer} (ex: 250) ou {@code null} se a entrada for nula
     */
    @Override
    public Integer convertToDatabaseColumn(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value
                .setScale(2, RoundingMode.UNNECESSARY)  // Garante escala de 2 casas decimais exatas
                .movePointRight(2)                            // Multiplica por 100 movendo a vírgula para a direita (ex: 2.50 -> 250)
                .intValueExact();                                // Converte para inteiro de 32 bits (INTEGER do Postgres)
    }

    /**
     * Converte o valor inteiro (centavos) armazenado no banco de dados para {@link BigDecimal}
     * no momento em que a entidade JPA é carregada em memória (SELECT).
     *
     * @param value O valor inteiro de centavos do banco (ex: 250)
     * @return O valor em formato {@link BigDecimal} (ex: 2.50) ou {@code null} se o banco retornar nulo
     */
    @Override
    public BigDecimal convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value, 2); // Reconstrói o BigDecimal informando escala de 2 casas decimais (250 -> 2.50)
    }
}

