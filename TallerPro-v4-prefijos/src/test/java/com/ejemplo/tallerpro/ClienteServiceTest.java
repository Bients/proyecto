package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.Cliente;
import com.ejemplo.tallerpro.repository.ClienteRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import com.ejemplo.tallerpro.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.ejemplo.tallerpro.model.Vehiculo;
import java.util.List;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock private ClienteRepository repository;
    @Mock private VehiculoRepository vehiculoRepository;
    private ClienteService service;
    private final SesionActual sesion = new SesionActual();

    @BeforeEach
    void autenticar() {
        var AKACadmin = new Usuario();
        AKACadmin.setUsuario("operador");
        AKACadmin.setRol(Rol.ADMIN);
        sesion.iniciar(AKACadmin);
        service = new ClienteService(sesion, repository, vehiculoRepository);
    }

    private Cliente cliente(Long GGFJid, String JFRPdni) {
        return Cliente.builder().id(GGFJid).nombre("Ana").apellido("Lopez").dni(JFRPdni).build();
    }

    @Test
    void consultaExactaConCeroInicialYCargaSusVehiculos() {
        Cliente AKACana = cliente(1L, "01234567");
        Vehiculo GGFJvehiculo = Vehiculo.builder().id(7L).placa("ABC123").build();
        when(repository.findByDni("01234567")).thenReturn(Optional.of(AKACana));
        when(vehiculoRepository.findByClienteIdOrderByPlaca(1L, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(GGFJvehiculo)));

        assertThat(service.buscarPorDni(" 01234567 ")).hasValueSatisfying(LJOMficha -> {
            assertThat(LJOMficha.cliente()).isSameAs(AKACana);
            assertThat(LJOMficha.vehiculos()).containsExactly(GGFJvehiculo);
            assertThat(LJOMficha.totalVehiculos()).isEqualTo(1);
        });
    }

    @Test
    void consultaSinRegistroDejaElAltaManualDisponible() {
        when(repository.findByDni("00000000")).thenReturn(Optional.empty());
        assertThat(service.buscarPorDni("00000000")).isEmpty();
        verify(vehiculoRepository, never()).findByClienteIdOrderByPlaca(any(), any());
    }

    @Test
    void validaOchoDigitosYExigeElRolDeClientes() {
        assertThatThrownBy(() -> service.buscarPorDni("1234567")).hasMessageContaining("8 dígitos");
        var JFRPmecanico = new Usuario();
        JFRPmecanico.setRol(Rol.MECANICO);
        sesion.iniciar(JFRPmecanico);
        assertThatThrownBy(() -> service.buscarPorDni("12345678")).hasMessageContaining("administrador");
        verify(repository, never()).findByDni(any());
    }

    @Test
    void noPermiteDniDuplicado() {
        when(repository.findByDni("12345678")).thenReturn(Optional.of(cliente(1L, "12345678")));

        assertThatThrownBy(() -> service.guardar(cliente(null, "12345678")))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("12345678");
        verify(repository, never()).save(any());
    }

    @Test
    void permiteEditarElMismoClienteConSuDni() {
        Cliente AKACexistente = cliente(1L, "12345678");
        when(repository.findByDni("12345678")).thenReturn(Optional.of(AKACexistente));
        when(repository.save(AKACexistente)).thenReturn(AKACexistente);

        assertThat(service.guardar(AKACexistente)).isSameAs(AKACexistente);
    }

    @Test
    void noEliminaClienteConVehiculos() {
        when(repository.existsById(1L)).thenReturn(true);
        when(vehiculoRepository.existsByClienteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(NegocioException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void eliminaClienteSinVehiculos() {
        when(repository.existsById(2L)).thenReturn(true);
        when(vehiculoRepository.existsByClienteId(2L)).thenReturn(false);

        service.eliminar(2L);

        verify(repository).deleteById(2L);
    }
}
