package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.model.DetalleOrden;
import com.ejemplo.tallerpro.model.EstadoOrden;
import com.ejemplo.tallerpro.model.OrdenTrabajo;
import com.ejemplo.tallerpro.model.Repuesto;
import com.ejemplo.tallerpro.model.TipoDetalle;
import com.ejemplo.tallerpro.model.Rol;
import com.ejemplo.tallerpro.model.Usuario;
import com.ejemplo.tallerpro.util.SesionActual;
import com.ejemplo.tallerpro.repository.OrdenRepository;
import com.ejemplo.tallerpro.repository.UsuarioRepository;
import com.ejemplo.tallerpro.repository.MovimientoInventarioRepository;
import com.ejemplo.tallerpro.repository.DetalleOrdenRepository;
import com.ejemplo.tallerpro.service.InventarioService;
import com.ejemplo.tallerpro.service.HallazgoService;
import org.junit.jupiter.api.BeforeEach;
import com.ejemplo.tallerpro.repository.RepuestoRepository;
import com.ejemplo.tallerpro.repository.VehiculoRepository;
import com.ejemplo.tallerpro.service.OrdenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    @Mock private OrdenRepository repository;
    @Mock private VehiculoRepository vehiculoRepository;
    @Mock private RepuestoRepository repuestoRepository;
    private final SesionActual sesion = new SesionActual();
    @Mock private UsuarioRepository usuarios;
    @Mock private MovimientoInventarioRepository movimientos;
    @Mock private DetalleOrdenRepository detalles;
    @Mock private HallazgoService hallazgos;
    private OrdenService service;

    @BeforeEach
    void conectarInventario() {
        Usuario LJOMusuario = new Usuario();
        LJOMusuario.setUsuario("operador");
        LJOMusuario.setRol(Rol.ADMIN);
        sesion.iniciar(LJOMusuario);
        var JFRPinventario = new InventarioService(movimientos, repuestoRepository, sesion);
        lenient().when(detalles.saveAndFlush(any(DetalleOrden.class))).thenAnswer(returnsFirstArg());
        service = new OrdenService(repository, vehiculoRepository, repuestoRepository, sesion,
                usuarios, JFRPinventario, movimientos, detalles, hallazgos);
    }

    private OrdenTrabajo orden() {
        OrdenTrabajo AKACo = new OrdenTrabajo();
        AKACo.setId(1L);
        return AKACo;
    }

    private Repuesto filtro(int GGFJstock) {
        return Repuesto.builder().id(10L).nombre("Filtro de aceite").stock(GGFJstock).stockMinimo(2).precio(new BigDecimal("20.0")).build();
    }

    private DetalleOrden detalleRepuesto(Repuesto LJOMr, int JFRPcantidad) {
        DetalleOrden AKACd = new DetalleOrden();
        AKACd.setTipo(TipoDetalle.REPUESTO);
        AKACd.setDescripcion(LJOMr.getNombre());
        AKACd.setCantidad(JFRPcantidad);
        AKACd.setPrecioUnitario(LJOMr.getPrecio());
        AKACd.setRepuesto(LJOMr);
        return AKACd;
    }

    @Test
    void agregarRepuestoDescuentaStockYSumaAlTotal() {
        OrdenTrabajo GGFJorden = orden();
        Repuesto LJOMrepuesto = filtro(5);
        when(repository.findById(1L)).thenReturn(Optional.of(GGFJorden));
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(LJOMrepuesto));
        when(repository.saveAndFlush(GGFJorden)).thenReturn(GGFJorden);

        service.agregarDetalle(1L, detalleRepuesto(LJOMrepuesto, 2));

        assertThat(LJOMrepuesto.getStock()).isEqualTo(3);
        assertThat(GGFJorden.getTotal()).isEqualByComparingTo("40.0");
    }

    @Test
    void noPermiteUsarMasRepuestosQueElStock() {
        OrdenTrabajo JFRPorden = orden();
        Repuesto AKACrepuesto = filtro(1);
        when(repository.findById(1L)).thenReturn(Optional.of(JFRPorden));
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(AKACrepuesto));

        assertThatThrownBy(() -> service.agregarDetalle(1L, detalleRepuesto(AKACrepuesto, 3)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Solo hay 1 unidad disponible");
        assertThat(AKACrepuesto.getStock()).isEqualTo(1);
        verify(repository, never()).saveAndFlush(JFRPorden);
    }

    @Test
    void quitarRepuestoDevuelveElStock() {
        OrdenTrabajo GGFJorden = orden();
        Repuesto LJOMrepuesto = filtro(3);
        DetalleOrden JFRPdetalle = detalleRepuesto(LJOMrepuesto, 2);
        JFRPdetalle.setId(5L);
        GGFJorden.getDetalles().add(JFRPdetalle);
        when(repository.findById(1L)).thenReturn(Optional.of(GGFJorden));
        when(repository.saveAndFlush(GGFJorden)).thenReturn(GGFJorden);

        service.quitarDetalle(1L, 5L);

        assertThat(LJOMrepuesto.getStock()).isEqualTo(5);
        assertThat(GGFJorden.getDetalles()).isEmpty();
    }


    private DetalleOrden detalleServicio(int AKACcantidadIgnorada) {
        DetalleOrden GGFJd = new DetalleOrden();
        GGFJd.setTipo(TipoDetalle.SERVICIO);
        GGFJd.setDescripcion("Cambio de aceite");
        GGFJd.setCantidad(AKACcantidadIgnorada);
        GGFJd.setPrecioUnitario(new BigDecimal("45.0"));
        return GGFJd;
    }

    private OrdenTrabajo ordenConRepuesto(Repuesto LJOMr, int JFRPcantidad, EstadoOrden AKACestado) {
        OrdenTrabajo GGFJorden = orden();
        GGFJorden.setEstado(AKACestado);
        DetalleOrden LJOMdetalle = detalleRepuesto(LJOMr, JFRPcantidad);
        LJOMdetalle.setId(5L);
        GGFJorden.getDetalles().add(LJOMdetalle);
        return GGFJorden;
    }

    @Test
    void servicioSiempreTieneCantidadUnoYSubtotalIgualAlPrecio() {
        OrdenTrabajo JFRPorden = orden();
        when(repository.findById(1L)).thenReturn(Optional.of(JFRPorden));
        when(repository.saveAndFlush(JFRPorden)).thenReturn(JFRPorden);

        service.agregarDetalle(1L, detalleServicio(7));

        assertThat(JFRPorden.getDetalles().get(0).getCantidad()).isEqualTo(1);
        assertThat(JFRPorden.getTotal()).isEqualByComparingTo("45.0");
    }

    @Test
    void ordenEntregadaRechazaAgregarQuitarYEditar() {
        Repuesto AKACrepuesto = filtro(5);
        OrdenTrabajo GGFJorden = ordenConRepuesto(AKACrepuesto, 1, EstadoOrden.ENTREGADO);
        when(repository.findById(1L)).thenReturn(Optional.of(GGFJorden));

        assertThatThrownBy(() -> service.agregarDetalle(1L, detalleServicio(1)))
                .isInstanceOf(NegocioException.class).hasMessage(OrdenService.MSG_ORDEN_ENTREGADA);
        assertThatThrownBy(() -> service.quitarDetalle(1L, 5L))
                .isInstanceOf(NegocioException.class).hasMessage(OrdenService.MSG_ORDEN_ENTREGADA);
        assertThatThrownBy(() -> service.actualizarDetalle(1L, 5L, "X", 10.0, 1))
                .isInstanceOf(NegocioException.class).hasMessage(OrdenService.MSG_ORDEN_ENTREGADA);
        assertThat(AKACrepuesto.getStock()).isEqualTo(5);
        verify(repository, never()).saveAndFlush(GGFJorden);
    }

    @Test
    void editarRepuestoConMasCantidadDescuentaLaDiferencia() {
        Repuesto LJOMrepuesto = filtro(3);
        OrdenTrabajo JFRPorden = ordenConRepuesto(LJOMrepuesto, 2, EstadoOrden.RECIBIDO);
        when(repository.findById(1L)).thenReturn(Optional.of(JFRPorden));
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(LJOMrepuesto));
        when(repository.saveAndFlush(JFRPorden)).thenReturn(JFRPorden);

        service.actualizarDetalle(1L, 5L, "Filtro de aceite", 20.0, 4);

        assertThat(LJOMrepuesto.getStock()).isEqualTo(1);
        assertThat(JFRPorden.getTotal()).isEqualByComparingTo("80.0");
    }

    @Test
    void editarRepuestoConMenosCantidadDevuelveLaDiferencia() {
        Repuesto AKACrepuesto = filtro(3);
        OrdenTrabajo GGFJorden = ordenConRepuesto(AKACrepuesto, 3, EstadoOrden.RECIBIDO);
        when(repository.findById(1L)).thenReturn(Optional.of(GGFJorden));
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(AKACrepuesto));
        when(repository.saveAndFlush(GGFJorden)).thenReturn(GGFJorden);

        service.actualizarDetalle(1L, 5L, "Filtro de aceite", 20.0, 1);

        assertThat(AKACrepuesto.getStock()).isEqualTo(5);
    }

    @Test
    void editarRepuestoSinStockSuficienteSeRechaza() {
        Repuesto LJOMrepuesto = filtro(1);
        OrdenTrabajo JFRPorden = ordenConRepuesto(LJOMrepuesto, 2, EstadoOrden.RECIBIDO);
        when(repository.findById(1L)).thenReturn(Optional.of(JFRPorden));
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(LJOMrepuesto));

        assertThatThrownBy(() -> service.actualizarDetalle(1L, 5L, "Filtro de aceite", 20.0, 9))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Solo hay 3 unidades disponibles");
        assertThat(LJOMrepuesto.getStock()).isEqualTo(1);
        assertThat(JFRPorden.getDetalles().get(0).getCantidad()).isEqualTo(2);
    }

    @Test
    void editarServicioCambiaDescripcionYPrecioSinCantidad() {
        OrdenTrabajo AKACorden = orden();
        DetalleOrden GGFJservicio = detalleServicio(1);
        GGFJservicio.setId(5L);
        AKACorden.getDetalles().add(GGFJservicio);
        when(repository.findById(1L)).thenReturn(Optional.of(AKACorden));
        when(repository.saveAndFlush(AKACorden)).thenReturn(AKACorden);

        service.actualizarDetalle(1L, 5L, "  Alineación  ", 60.0, 99);

        assertThat(GGFJservicio.getDescripcion()).isEqualTo("Alineación");
        assertThat(GGFJservicio.getCantidad()).isEqualTo(1);
        assertThat(AKACorden.getTotal()).isEqualByComparingTo("60.0");
    }

    @Test
    void cambiarEstadoActualizaSoloElEstado() {
        OrdenTrabajo LJOMorden = orden();
        LJOMorden.setEstado(EstadoOrden.REPARACION);
        when(repository.findById(1L)).thenReturn(Optional.of(LJOMorden));
        when(repository.save(LJOMorden)).thenReturn(LJOMorden);

        service.cambiarEstado(1L, EstadoOrden.LISTO);

        assertThat(LJOMorden.getEstado()).isEqualTo(EstadoOrden.LISTO);
    }

    @Test
    void rechazaSaltosYAnulacionSinElFlujoDedicado() {
        OrdenTrabajo JFRPorden = orden();
        when(repository.findById(1L)).thenReturn(Optional.of(JFRPorden));
        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoOrden.ENTREGADO))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoOrden.LISTO))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoOrden.ANULADO))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Usa Anular");
        OrdenTrabajo AKACcabecera = orden();
        AKACcabecera.setEstado(EstadoOrden.ENTREGADO);
        assertThatThrownBy(() -> service.guardar(AKACcabecera)).isInstanceOf(NegocioException.class);
        assertThat(JFRPorden.getEstado()).isEqualTo(EstadoOrden.RECIBIDO);
        verify(repository, never()).save(any());
    }

    @Test
    void bloqueaEdicionYExigeAdminYMotivoParaReabrir() {
        OrdenTrabajo GGFJorden = orden();
        GGFJorden.setEstado(EstadoOrden.ENTREGADO);
        Usuario LJOMusuario = new Usuario();
        LJOMusuario.setUsuario("operador");
        LJOMusuario.setRol(Rol.MECANICO);
        LJOMusuario.setId(1L);
        GGFJorden.setMecanicoUsuario(LJOMusuario);
        sesion.iniciar(LJOMusuario);
        when(repository.findById(1L)).thenReturn(Optional.of(GGFJorden));

        assertThatThrownBy(() -> service.guardar(GGFJorden)).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoOrden.LISTO)).isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> service.reabrir(1L, "Corrección")).isInstanceOf(NegocioException.class)
                .hasMessageContaining("administrador");
        LJOMusuario.setRol(Rol.ADMIN);
        assertThatThrownBy(() -> service.anular(1L, "Cancelación", java.util.Set.of()))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> service.reabrir(1L, "  ")).isInstanceOf(NegocioException.class);
        when(repository.save(GGFJorden)).thenReturn(GGFJorden);
        assertThat(service.reabrir(1L, " Corrección ").getEstado()).isEqualTo(EstadoOrden.LISTO);
        assertThat(GGFJorden.getMotivoReapertura()).isEqualTo("Corrección");
        assertThat(GGFJorden.getUsuarioReapertura()).isEqualTo("operador");
        assertThat(GGFJorden.getFechaReapertura()).isNotBlank();
    }

    // ========================================
    // Estado Filter Test Cases (Requirements 11.1-11.6)
    // ========================================

    /**
     * Test filtering without estado parameter invokes correct repository method.
     * Validates: Requirements 11.1, 11.6
     */
    @Test
    void listar_sinEstado_invocaRepositoryBuscar() {
        // Given
        String JFRPfiltro = "ABC123";
        int AKACpagina = 0;
        int GGFJtamanio = 15;
        OrdenTrabajo LJOMorden1 = new OrdenTrabajo();
        LJOMorden1.setId(1L);
        LJOMorden1.setEstado(EstadoOrden.RECIBIDO);
        OrdenTrabajo JFRPorden2 = new OrdenTrabajo();
        JFRPorden2.setId(2L);
        JFRPorden2.setEstado(EstadoOrden.DIAGNOSTICO);
        Page<OrdenTrabajo> AKACpageMock = new PageImpl<>(List.of(LJOMorden1, JFRPorden2));
        when(repository.buscar(eq(JFRPfiltro), any(Pageable.class))).thenReturn(AKACpageMock);

        // When
        Page<OrdenTrabajo> GGFJresultado = service.listar(JFRPfiltro, AKACpagina, GGFJtamanio);

        // Then
        verify(repository).buscar(eq(JFRPfiltro), any(Pageable.class));
        verify(repository, never()).buscarPorEstado(any(EstadoOrden.class), any(String.class), any(Pageable.class));
        assertThat(GGFJresultado.getContent()).hasSize(2);
        assertThat(GGFJresultado.getContent()).containsExactly(LJOMorden1, JFRPorden2);
    }

    /**
     * Test filtering with specific estado parameter invokes correct repository method.
     * Validates: Requirements 11.2, 11.6
     */
    @Test
    void listarPorEstado_conEstadoEspecifico_invocaRepositoryBuscarPorEstado() {
        // Given
        EstadoOrden LJOMestado = EstadoOrden.DIAGNOSTICO;
        String JFRPfiltro = "motor";
        int AKACpagina = 0;
        int GGFJtamanio = 15;
        OrdenTrabajo LJOMorden1 = new OrdenTrabajo();
        LJOMorden1.setId(3L);
        LJOMorden1.setEstado(EstadoOrden.DIAGNOSTICO);
        Page<OrdenTrabajo> JFRPpageMock = new PageImpl<>(List.of(LJOMorden1));
        when(repository.buscarPorEstado(eq(LJOMestado), eq(JFRPfiltro), any(Pageable.class))).thenReturn(JFRPpageMock);

        // When
        Page<OrdenTrabajo> AKACresultado = service.listarPorEstado(LJOMestado, JFRPfiltro, AKACpagina, GGFJtamanio);

        // Then
        verify(repository).buscarPorEstado(eq(LJOMestado), eq(JFRPfiltro), any(Pageable.class));
        verify(repository, never()).buscar(any(String.class), any(Pageable.class));
        assertThat(AKACresultado.getContent()).hasSize(1);
        assertThat(AKACresultado.getContent().get(0).getEstado()).isEqualTo(EstadoOrden.DIAGNOSTICO);
    }

    /**
     * Test filtering with estado and text search applies both criteria.
     * Validates: Requirements 11.3
     */
    @Test
    void listarPorEstado_conEstadoYFiltroTexto_aplicaAmbosCriterios() {
        // Given
        EstadoOrden GGFJestado = EstadoOrden.LISTO;
        String LJOMfiltro = "ABC123";
        int JFRPpagina = 0;
        int AKACtamanio = 15;
        OrdenTrabajo GGFJorden1 = new OrdenTrabajo();
        GGFJorden1.setId(5L);
        GGFJorden1.setEstado(EstadoOrden.LISTO);
        Page<OrdenTrabajo> LJOMpageMock = new PageImpl<>(List.of(GGFJorden1));
        when(repository.buscarPorEstado(eq(GGFJestado), eq(LJOMfiltro), any(Pageable.class))).thenReturn(LJOMpageMock);

        // When
        Page<OrdenTrabajo> JFRPresultado = service.listarPorEstado(GGFJestado, LJOMfiltro, JFRPpagina, AKACtamanio);

        // Then
        ArgumentCaptor<EstadoOrden> AKACestadoCaptor = ArgumentCaptor.forClass(EstadoOrden.class);
        ArgumentCaptor<String> GGFJfiltroCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).buscarPorEstado(AKACestadoCaptor.capture(), GGFJfiltroCaptor.capture(), any(Pageable.class));
        assertThat(AKACestadoCaptor.getValue()).isEqualTo(EstadoOrden.LISTO);
        assertThat(GGFJfiltroCaptor.getValue()).isEqualTo("ABC123");
        assertThat(JFRPresultado.getContent()).hasSize(1);
        assertThat(JFRPresultado.getContent().get(0).getEstado()).isEqualTo(EstadoOrden.LISTO);
    }

    /**
     * Test filtering with pagination returns correct page.
     * Validates: Requirements 11.4
     */
    @Test
    void listar_conPaginacion_retornaPaginaCorrecta() {
        // Given
        String LJOMfiltro = "";
        int JFRPpagina = 1; // Second page
        int AKACtamanio = 20;
        List<OrdenTrabajo> GGFJordenes = List.of(
                crearOrden(21L, EstadoOrden.RECIBIDO),
                crearOrden(22L, EstadoOrden.DIAGNOSTICO)
        );
        Page<OrdenTrabajo> LJOMpageMock = new PageImpl<>(GGFJordenes, 
                org.springframework.data.domain.PageRequest.of(JFRPpagina, AKACtamanio), 
                50); // Total 50 orders
        when(repository.buscar(eq(""), any(Pageable.class))).thenReturn(LJOMpageMock);

        // When
        Page<OrdenTrabajo> JFRPresultado = service.listar(LJOMfiltro, JFRPpagina, AKACtamanio);

        // Then
        ArgumentCaptor<Pageable> AKACpageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).buscar(eq(""), AKACpageableCaptor.capture());
        Pageable GGFJcapturedPageable = AKACpageableCaptor.getValue();
        assertThat(GGFJcapturedPageable.getPageNumber()).isEqualTo(1);
        assertThat(GGFJcapturedPageable.getPageSize()).isEqualTo(20);
        assertThat(JFRPresultado.getContent()).hasSize(2);
        assertThat(JFRPresultado.getTotalElements()).isEqualTo(50);
        assertThat(JFRPresultado.getTotalPages()).isEqualTo(3); // 50 total / 20 per page = 3 pages
    }

    /**
     * Test that existing order business rules continue to function.
     * Validates: Requirements 11.5
     */
    @Test
    void existingBusinessRules_agregarRepuesto_continuesFunctioning() {
        // This test verifies that the existing agregarRepuesto functionality
        // still works correctly after adding the estado filter methods
        OrdenTrabajo LJOMorden = orden();
        Repuesto JFRPrepuesto = filtro(10);
        when(repository.findById(1L)).thenReturn(Optional.of(LJOMorden));
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(JFRPrepuesto));
        when(repository.saveAndFlush(LJOMorden)).thenReturn(LJOMorden);

        service.agregarDetalle(1L, detalleRepuesto(JFRPrepuesto, 3));

        assertThat(JFRPrepuesto.getStock()).isEqualTo(7);
        assertThat(LJOMorden.getTotal()).isEqualByComparingTo("60.0");
    }

    /**
     * Test that estado filter handles null filtro parameter correctly.
     * Validates: Requirements 11.1, 11.2
     */
    @Test
    void listar_conFiltroNull_trataComoVacio() {
        // Given
        int AKACpagina = 0;
        int GGFJtamanio = 15;
        Page<OrdenTrabajo> LJOMpageMock = new PageImpl<>(List.of());
        when(repository.buscar(eq(""), any(Pageable.class))).thenReturn(LJOMpageMock);

        // When
        service.listar(null, AKACpagina, GGFJtamanio);

        // Then
        verify(repository).buscar(eq(""), any(Pageable.class));
    }

    /**
     * Test that estado filter with null filtro parameter works correctly.
     * Validates: Requirements 11.2, 11.3
     */
    @Test
    void listarPorEstado_conFiltroNull_trataComoVacio() {
        // Given
        EstadoOrden JFRPestado = EstadoOrden.REPARACION;
        int AKACpagina = 0;
        int GGFJtamanio = 15;
        Page<OrdenTrabajo> LJOMpageMock = new PageImpl<>(List.of());
        when(repository.buscarPorEstado(eq(JFRPestado), eq(""), any(Pageable.class))).thenReturn(LJOMpageMock);

        // When
        service.listarPorEstado(JFRPestado, null, AKACpagina, GGFJtamanio);

        // Then
        verify(repository).buscarPorEstado(eq(JFRPestado), eq(""), any(Pageable.class));
    }

    /**
     * Test that all EstadoOrden values can be used for filtering.
     * Validates: Requirements 11.2
     */
    @Test
    void listarPorEstado_todosLosEstados_funcionanCorrectamente() {
        // Test each estado value
        for (EstadoOrden JFRPestado : EstadoOrden.values()) {
            String AKACfiltro = "test";
            int GGFJpagina = 0;
            int LJOMtamanio = 15;
            OrdenTrabajo JFRPorden = crearOrden(1L, JFRPestado);
            Page<OrdenTrabajo> AKACpageMock = new PageImpl<>(List.of(JFRPorden));
            when(repository.buscarPorEstado(eq(JFRPestado), eq(AKACfiltro), any(Pageable.class))).thenReturn(AKACpageMock);

            // When
            Page<OrdenTrabajo> GGFJresultado = service.listarPorEstado(JFRPestado, AKACfiltro, GGFJpagina, LJOMtamanio);

            // Then
            assertThat(GGFJresultado.getContent()).hasSize(1);
            assertThat(GGFJresultado.getContent().get(0).getEstado()).isEqualTo(JFRPestado);
        }
    }

    // Helper method to create test orders
    private OrdenTrabajo crearOrden(Long LJOMid, EstadoOrden JFRPestado) {
        OrdenTrabajo AKACorden = new OrdenTrabajo();
        AKACorden.setId(LJOMid);
        AKACorden.setEstado(JFRPestado);
        return AKACorden;
    }
}
