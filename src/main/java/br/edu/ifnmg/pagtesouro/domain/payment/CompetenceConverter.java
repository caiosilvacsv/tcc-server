package br.edu.ifnmg.pagtesouro.domain.payment;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Conversor JPA responsável por mapear a competência financeira entre a aplicação (String MMYYYY ou YYYYMM)
 * e o banco de dados PostgreSQL (INTEGER).
 * <p>
 * Evita erros de colisão de tipos ("column competence is of type integer but expression is of type character varying")
 * sem exigir alterações nas regras de negócio e DTOs de integração.
 * </p>
 *
 * @author Caio da Silva Viana
 */
@Converter
public class CompetenceConverter implements AttributeConverter<String, Integer> {

    @Override
    public Integer convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(attribute);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return String.format("%06d", dbData);
    }
}
