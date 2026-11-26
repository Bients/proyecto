package com.ejemplo.tallerpro.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class CentimosConverter implements AttributeConverter<BigDecimal, Long> {

    public static final BigDecimal CERO = new BigDecimal("0.00");

    public static BigDecimal redondear(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Long convertToDatabaseColumn(BigDecimal importe) {
        return importe == null ? null : redondear(importe).movePointRight(2).longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long centimos) {
        return centimos == null ? null : BigDecimal.valueOf(centimos, 2);
    }
}
