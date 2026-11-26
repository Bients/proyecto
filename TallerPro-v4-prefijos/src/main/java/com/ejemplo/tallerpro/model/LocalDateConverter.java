package com.ejemplo.tallerpro.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/** Guarda las fechas como texto ISO (2026-11-03): es lo mas seguro con SQLite. */
@Converter
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto);
    }
}
