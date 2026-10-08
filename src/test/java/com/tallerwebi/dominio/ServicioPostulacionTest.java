package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPostulacionTest {

  private RepositorioUsuario usuarios;
  private RepositorioVacante vacantes;
  private RepositorioPostulacion postulaciones;
  private ServicioPostulacion servicio;
  private VacanteDTO oferta;

  @BeforeEach
  public void init() {
    usuarios = mock(RepositorioUsuario.class);
    vacantes = mock(RepositorioVacante.class);
    postulaciones = mock(RepositorioPostulacion.class);
    servicio = new ServicioPostulacionImpl(usuarios, postulaciones, vacantes);
    Usuario usuario = new Usuario();
    usuario.setRol("CANDIDATO");
    when(usuarios.buscarPorId(1L)).thenReturn(usuario);
    when(postulaciones.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    oferta = new VacanteDTO();
    oferta.setId(10L);
    oferta.setFuente("JOBICY");
    oferta.setTitulo("Desarrollador Java");
    oferta.setDescripcion("Descripción de la oferta");
  }

  @Test
  public void deberiaGuardarLaOfertaExternaAlPostularse() {
    Postulacion resultado = servicio.registrarEnVacante(1L, oferta);

    assertEquals("JOBICY:10", resultado.getOfertaId());
    assertEquals("Registrada", resultado.getEstado());
    assertEquals(oferta.getDescripcion(), resultado.getVacante().getDescripcion());
    assertEquals(oferta.getId(), resultado.getVacante().getIdExterno());
    verify(vacantes).guardar(resultado.getVacante());
  }

  @Test
  public void deberiaReutilizarUnaOfertaExternaGuardada() {
    Vacante guardada = new Vacante();
    when(vacantes.buscarPorFuenteEIdExterno("JOBICY", 10L)).thenReturn(guardada);

    Postulacion resultado = servicio.registrarEnVacante(1L, oferta);

    assertSame(guardada, resultado.getVacante());
    verify(vacantes, never()).guardar(any());
  }

  @Test
  public void deberiaRelacionarLaVacanteLocalExistente() {
    oferta.setFuente("LOCAL");
    Vacante local = new Vacante();
    when(vacantes.obtenerPorId(10L)).thenReturn(local);

    Postulacion resultado = servicio.registrarEnVacante(1L, oferta);

    assertSame(local, resultado.getVacante());
    verify(vacantes, never()).guardar(any());
  }

  @Test
  public void noDeberiaRepetirUnaPostulacion() {
    Postulacion existente = new Postulacion();
    when(postulaciones.buscarPorUsuarioYOferta(1L, "JOBICY:10")).thenReturn(existente);

    assertSame(existente, servicio.registrarEnVacante(1L, oferta));
    verify(postulaciones, never()).guardar(any());
  }

  @Test
  public void noDeberiaPostularseAUnaVacanteLocalInactiva() {
    oferta.setFuente("LOCAL");
    Vacante local = new Vacante();
    local.setActiva(false);
    when(vacantes.obtenerPorId(10L)).thenReturn(local);

    assertThrows(IllegalArgumentException.class, () -> servicio.registrarEnVacante(1L, oferta));
    verify(postulaciones, never()).guardar(any());
  }
}
