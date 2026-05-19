package br.edu.ifnmg.pagtesouro.domain.ticket;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class MoneyCentsConverter implements AttributeConverter<BigDecimal, Long> {

  @Override
  public Long convertToDatabaseColumn(BigDecimal value) {
    if (value == null) {
      return null;
    }

    return value
        .setScale(2, RoundingMode.UNNECESSARY)
        .movePointRight(2)
        .longValueExact();
  }

  @Override
  public BigDecimal convertToEntityAttribute(Long value) {
    if (value == null) {
      return null;
    }

    return BigDecimal.valueOf(value, 2);
  }
}
