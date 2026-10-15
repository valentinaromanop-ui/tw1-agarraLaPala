package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioOfertaEmpleadorTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private RepositorioPostulacion repositorioPostulacion;
  private ServicioOfertaEmpleador servicio;

  @BeforeEach
  void iniciar() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    repositorioPostulacion = mock(RepositorioPostulacion.class);
    servicio =
      new ServicioOfertaEmpleadorImpl(
        repositorioUsuario,
        repositorioVacante,
        repositorioPostulacion
      );
  }

  @Test
  void empleadorPuedeCerrarSuVacanteAbierta() {
    Vacante vacante = crearVacante(42L);
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(vacante);

    servicio.cerrar(10L, 42L);

    assertEquals(EstadoVacante.CERRADA, vacante.getEstado());
    verify(repositorioVacante).obtenerPorIdParaActualizar(10L);
  }

  @Test
  void empleadorNoPuedeCerrarVacanteAjena() {
    Vacante vacante = crearVacante(99L);
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(vacante);

    assertThrows(AccesoOfertaDenegadoException.class, () -> servicio.cerrar(10L, 42L));
    assertEquals(EstadoVacante.ABIERTA, vacante.getEstado());
  }

  @Test
  void noSePuedeCerrarUnaVacanteQueYaEstaCerrada() {
    Vacante vacante = crearVacante(42L);
    vacante.setEstado(EstadoVacante.CERRADA);
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(vacante);

    assertThrows(OfertaCerradaException.class, () -> servicio.cerrar(10L, 42L));
  }

  @Test
  void cerrarUnaVacanteInexistenteFalla() {
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(null);

    assertThrows(VacanteNoEncontradaException.class, () -> servicio.cerrar(10L, 42L));
  }

  private Vacante crearVacante(Long empleadorId) {
    Vacante vacante = new Vacante();
    vacante.setId(10L);
    vacante.setEmpleadorId(empleadorId);
    return vacante;
  }
}
