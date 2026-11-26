package com.ejemplo.tallerpro.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class DecimalTextoConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal valor) {
        return valor == null ? null : valor.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public BigDecimal convertToEntityAttribute(String valor) {
        return valor == null ? null : new BigDecimal(valor).setScale(6, RoundingMode.HALF_UP);
    }
}
