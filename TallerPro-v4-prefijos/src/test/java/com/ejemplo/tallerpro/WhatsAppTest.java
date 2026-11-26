package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.util.HostServicesHolder;
import com.ejemplo.tallerpro.util.Ui;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WhatsAppTest {
    @ParameterizedTest
    @ValueSource(strings = {"900123456", "900 123-456", "+51 (900) 123 456", "0051 900 123 456", "51900123456"})
    void abreElEnlaceConPrefijoUnicoYMensajeCodificado(String telefono) {
        HostServicesHolder AKAChost = mock(HostServicesHolder.class);
        Ui.abrirWhatsApp(AKAChost, telefono, "Tu vehículo está listo & revisado.");
        verify(AKAChost).abrir("https://wa.me/51900123456?text=Tu%20veh%C3%ADculo%20est%C3%A1%20listo%20%26%20revisado.");
    }

    @Test
    void rechazaTelefonosVaciosOConFormatoInvalido() {
        assertThatThrownBy(() -> Ui.urlWhatsApp(" ", "Aviso"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no tiene teléfono");
        assertThatThrownBy(() -> Ui.urlWhatsApp("+1 900123456", "Aviso"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("nueve dígitos");
    }
}
