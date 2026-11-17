package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioReporteImplTest {

  private RepositorioReporte repositorioReporte;
  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private ServicioReporteImpl servicio;

  @BeforeEach
  public void init() {
    repositorioReporte = mock(RepositorioReporte.class);
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    servicio = new ServicioReporteImpl(repositorioReporte, repositorioUsuario, repositorioVacante);
  }

  private Reporte reporteDeOferta(Long reportanteId) {
    Reporte reporte = new Reporte();
    reporte.setTipo(TipoReporte.OFERTA);
    reporte.setObjetoId(7L);
    reporte.setUsuarioReportadoId(20L);
    reporte.setReportanteId(reportanteId);
    return reporte;
  }

  @Test
  public void reportarDeberiaGuardarSiNoSuperaElLimiteDiario() {
    when(repositorioReporte.contarDesde(eq(1L), any())).thenReturn(4L);

    servicio.reportar(TipoReporte.OFERTA, 7L, 20L, 1L, "ofensiva", "Oferta rara");

    verify(repositorioReporte, times(1)).guardar(any(Reporte.class));
  }

  @Test
  public void reportarDeberiaLanzarExcepcionAlSuperarElLimiteDiario() {
    when(repositorioReporte.contarDesde(eq(1L), any())).thenReturn(5L);

    assertThrows(
      LimiteReportesAlcanzadoException.class,
      () -> servicio.reportar(TipoReporte.OFERTA, 7L, 20L, 1L, "spam", "x")
    );
    verify(repositorioReporte, never()).guardar(any(Reporte.class));
  }

  @Test
  public void reportarDeberiaIgnorarReporteDuplicado() {
    when(repositorioReporte.existe(TipoReporte.OFERTA, 7L, 1L)).thenReturn(true);

    servicio.reportar(TipoReporte.OFERTA, 7L, 20L, 1L, "spam", "x");

    verify(repositorioReporte, never()).guardar(any(Reporte.class));
  }

  @Test
  public void reportarDeberiaIgnorarAutoReporte() {
    servicio.reportar(TipoReporte.CANDIDATO, 5L, 5L, 5L, "spam", "x");

    verify(repositorioReporte, never()).guardar(any(Reporte.class));
  }

  @Test
  public void registrarAutomaticoDeberiaGuardarSinReportante() {
    servicio.registrarAutomatico(TipoReporte.MENSAJE, 3L, 20L, "Palabra no permitida", "texto");

    ArgumentCaptor<Reporte> captor = ArgumentCaptor.forClass(Reporte.class);
    verify(repositorioReporte).guardar(captor.capture());
    assertNull(captor.getValue().getReportanteId());
    assertEquals(EstadoReporte.PENDIENTE, captor.getValue().getEstado());
    assertEquals(TipoReporte.MENSAJE, captor.getValue().getTipo());
  }

  @Test
  public void bloquearContenidoDeberiaDesactivarLaOfertaYResolverElReporte() {
    Reporte reporte = reporteDeOferta(1L);
    Vacante vacante = new Vacante();
    vacante.setActiva(true);
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);
    when(repositorioVacante.obtenerPorId(7L)).thenReturn(vacante);

    servicio.bloquearContenido(9L);

    assertFalse(vacante.getActiva());
    assertEquals(EstadoReporte.RESUELTO, reporte.getEstado());
  }

  @Test
  public void bloquearContenidoDeberiaBloquearAlUsuarioAlLlegarAlLimite() {
    Reporte reporte = reporteDeOferta(1L);
    Usuario empleador = new Usuario();
    empleador.setRol("EMPLEADOR");
    Vacante otraOferta = new Vacante();
    otraOferta.setActiva(true);
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);
    when(repositorioReporte.contarResueltosContra(20L))
      .thenReturn((long) ServicioReporteImpl.LIMITE_BLOQUEO_AUTOMATICO);
    when(repositorioUsuario.buscarPorId(20L)).thenReturn(empleador);
    when(repositorioVacante.buscarPorEmpleador(20L)).thenReturn(List.of(otraOferta));

    servicio.bloquearContenido(9L);

    assertTrue(empleador.getBloqueado());
    assertFalse(otraOferta.getActiva());
  }

  @Test
  public void bloquearContenidoNoDeberiaBloquearAlUsuarioBajoElLimite() {
    Reporte reporte = reporteDeOferta(1L);
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);
    when(repositorioReporte.contarResueltosContra(20L)).thenReturn(2L);

    servicio.bloquearContenido(9L);

    verify(repositorioUsuario, never()).buscarPorId(20L);
  }

  @Test
  public void bloquearUsuarioDeberiaBloquearloYDesactivarSusOfertas() {
    Reporte reporte = reporteDeOferta(1L);
    Usuario empleador = new Usuario();
    empleador.setRol("EMPLEADOR");
    Vacante oferta = new Vacante();
    oferta.setActiva(true);
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);
    when(repositorioUsuario.buscarPorId(20L)).thenReturn(empleador);
    when(repositorioVacante.buscarPorEmpleador(20L)).thenReturn(List.of(oferta));

    servicio.bloquearUsuario(9L);

    assertTrue(empleador.getBloqueado());
    assertFalse(oferta.getActiva());
    assertEquals(EstadoReporte.RESUELTO, reporte.getEstado());
  }

  @Test
  public void bloquearUsuarioNoDeberiaBloquearAUnAdmin() {
    Reporte reporte = reporteDeOferta(1L);
    Usuario admin = new Usuario();
    admin.setRol("ADMIN");
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);
    when(repositorioUsuario.buscarPorId(20L)).thenReturn(admin);

    servicio.bloquearUsuario(9L);

    assertFalse(Boolean.TRUE.equals(admin.getBloqueado()));
  }

  @Test
  public void descartarDeberiaReactivarLaOfertaOcultadaAutomaticamente() {
    Reporte reporte = reporteDeOferta(null);
    Vacante vacante = new Vacante();
    vacante.setActiva(false);
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);
    when(repositorioVacante.obtenerPorId(7L)).thenReturn(vacante);

    servicio.descartar(9L);

    assertTrue(vacante.getActiva());
    assertEquals(EstadoReporte.DESCARTADO, reporte.getEstado());
  }

  @Test
  public void descartarNoDeberiaTocarLaOfertaSiElReporteLoHizoUnaPersona() {
    Reporte reporte = reporteDeOferta(1L);
    when(repositorioReporte.obtener(9L)).thenReturn(reporte);

    servicio.descartar(9L);

    verify(repositorioVacante, never()).obtenerPorId(any());
    assertEquals(EstadoReporte.DESCARTADO, reporte.getEstado());
  }

  @Test
  public void deberiaLanzarExcepcionSiElReporteNoExiste() {
    when(repositorioReporte.obtener(99L)).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> servicio.descartar(99L));
  }
}
