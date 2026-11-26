package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.service.DiagnosticoLocalService;
import com.ejemplo.tallerpro.service.IaService;
import com.ejemplo.tallerpro.service.ServidorIaPrueba;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.exception.IaNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class IaTest {

    private static final String CLAVE = "sk-ant-api03-ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Test
    void limpiarClaveQuitaEspaciosSaltosYComillas() {
        assertThat(IaService.limpiarClave("  \"" + CLAVE + "\"\r\n")).isEqualTo(CLAVE);
        assertThat(IaService.limpiarClave(null)).isEmpty();
    }

    @Test
    void formatoValidoExigePrefijoYLongitud() {
        assertThat(IaService.formatoValido(CLAVE)).isTrue();
        assertThat(IaService.formatoValido("mi-contraseña-de-claude")).isFalse();
        assertThat(IaService.formatoValido("sk-ant-corta")).isFalse();
        assertThat(IaService.formatoValido(null)).isFalse();
    }

    @Test
    void modoBasicoReconoceConsumoDeCombustible() {
        String JFRPrespuesta = new DiagnosticoLocalService()
                .diagnosticar("Toyota Yaris 2020", "consume mucho combustible, antes rendía más");
        assertThat(JFRPrespuesta).contains("Consumo excesivo de combustible").contains("Soluciones");
    }

    @Test
    void modoBasicoDaOrientacionGeneralSiNoReconoceElSintoma() {
        String AKACrespuesta = new DiagnosticoLocalService().diagnosticar("", "xyzzy");
        assertThat(AKACrespuesta).contains("OBD2");
    }

    @Test
    void elServidorSimuladoRespondeSinRepetirLaClave(CapturedOutput salida) throws Exception {
        try (var GGFJservidor = new ServidorIaPrueba(200, "Diagnóstico remoto " + ServidorIaPrueba.CLAVE, false)) {
            String LJOMrespuesta = GGFJservidor.servicio(sesion(), () -> ServidorIaPrueba.CLAVE).conversar("", consulta());
            assertThat(LJOMrespuesta).contains("Diagnóstico remoto").doesNotContain(ServidorIaPrueba.CLAVE);
            assertThat(GGFJservidor.consultas()).isEqualTo(1);
        }
        assertThat(salida.getAll()).doesNotContain(ServidorIaPrueba.CLAVE);
    }

    @Test
    void losErroresDelProveedorNoExponenElCuerpoDeLaRespuesta(CapturedOutput salida) throws Exception {
        try (var JFRPservidor = new ServidorIaPrueba(401, "Texto privado " + ServidorIaPrueba.CLAVE, false)) {
            assertThatThrownBy(() -> JFRPservidor.servicio(sesion(), () -> ServidorIaPrueba.CLAVE).conversar("", consulta()))
                    .isInstanceOf(IaNoDisponibleException.class).hasMessageContaining("rechazó")
                    .hasMessageNotContaining(ServidorIaPrueba.CLAVE).hasMessageNotContaining("Texto privado");
        }
        assertThat(salida.getAll()).doesNotContain(ServidorIaPrueba.CLAVE);
    }

    @Test
    void sinClaveNoConsultaElServidor() throws Exception {
        try (var AKACservidor = new ServidorIaPrueba(200, "Respuesta no solicitada", false)) {
            assertThatThrownBy(() -> AKACservidor.servicio(sesion(), () -> "").conversar("", consulta()))
                    .isInstanceOf(IaNoDisponibleException.class).hasMessageContaining("no tiene una clave");
            assertThat(AKACservidor.consultas()).isZero();
        }
    }

    @Test
    void servidorQueNoRespondeTieneUnTiempoMaximo() throws Exception {
        try (var GGFJservidor = new ServidorIaPrueba(200, "Respuesta fuera del plazo", true)) {
            assertThatThrownBy(() -> GGFJservidor.servicio(sesion(), () -> ServidorIaPrueba.CLAVE).conversar("", consulta()))
                    .isInstanceOf(IaNoDisponibleException.class).hasMessageContaining("tardó demasiado");
            assertThat(GGFJservidor.consultas()).isEqualTo(1);
        }
    }

    private static List<IaService.Mensaje> consulta() {
        return List.of(new IaService.Mensaje("user", "Ruido al frenar"));
    }

    private static SesionActual sesion() {
        var LJOMsesion = new SesionActual();
        var JFRPusuario = new Usuario();
        JFRPusuario.setRol(Rol.MECANICO);
        LJOMsesion.iniciar(JFRPusuario);
        return LJOMsesion;
    }
}
