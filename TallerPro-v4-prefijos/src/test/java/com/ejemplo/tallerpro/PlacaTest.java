package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.util.Placa;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlacaTest {

    @ParameterizedTest
    @ValueSource(strings = {" ABC-123 ", "abc123", "aBc - 123", "ABC\t-\u00a0123"})
    void unificaEspaciosGuionYMayusculas(String entrada) {
        assertThat(Placa.normalizar(entrada)).isEqualTo("ABC123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" -  ", "\t"})
    void normalizaLaEntradaVacia(String entrada) {
        assertThat(Placa.normalizar(entrada)).isEmpty();
    }
}
