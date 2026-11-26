package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.util.Ui;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ImportesUiTest {
    @ParameterizedTest
    @CsvSource({
            "0, S/ 0.00",
            "55, S/ 55.00",
            "20, S/ 20.00",
            "100000.01, S/ 100000.01",
            "999999.99, S/ 999999.99",
            "-123456.78, S/ -123456.78",
            "123456.785, S/ 123456.79"
    })
    void conservaTodosLosDigitosYDosDecimales(String valor, String esperado) {
        assertThat(Ui.moneda(new BigDecimal(valor))).isEqualTo(esperado);
    }

    @Test
    void conservaElFormatoEnLlamadasExistentesConDouble() {
        assertThat(Ui.moneda(234567.89)).isEqualTo("S/ 234567.89");
    }
}
