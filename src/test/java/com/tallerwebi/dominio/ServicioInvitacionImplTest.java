package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioInvitacionImplTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private RepositorioPostulacion repositorioPostulacion;
  private RepositorioInvitacion repositorioInvitacion;
  private ServicioPerfil servicioPerfil;
  private ServicioNotificacionOferta servicioNotificacionOferta;
  private ServicioInvitacionImpl servicio;
  private Usuario empleador;
  private Usuario candidato;
  private Vacante oferta;

  @BeforeEach
  void iniciar() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    repositorioPostulacion = mock(RepositorioPostulacion.class);
    repositorioInvitacion = mock(RepositorioInvitacion.class);
    servicioPerfil = mock(ServicioPerfil.class);
    servicioNotificacionOferta = mock(ServicioNotificacionOferta.class);
    servicio =
      new ServicioInvitacionImpl(
        repositorioUsuario,
        repositorioVacante,
        repositorioPostulacion,
        repositorioInvitacion,
        servicioPerfil,
        servicioNotificacionOferta
      );

    empleador = new Usuario();
    empleador.setId(10L);
    empleador.setRol("EMPLEADOR");
    empleador.setEmpresa("Empresa Ejemplo");
    candidato = new Usuario();
    candidato.setId(20L);
    candidato.setRol("CANDIDATO");
    oferta = new Vacante();
    oferta.setId(5L);
    oferta.setEmpleadorId(10L);
    oferta.setTitulo("Desarrollador Java");
    oferta.setEmpresa("Empresa Ejemplo");
    oferta.setActiva(true);
    oferta.setEstado(EstadoVacante.ABIERTA);
    oferta.setLimitePostulaciones(3);
    Skill skill = new Skill();
    skill.setNombre("Java");
    VacanteSkill relacion = new VacanteSkill();
    relacion.setSkill(skill);
    relacion.setVacante(oferta);
    oferta.setVacanteSkills(Set.of(relacion));

    when(repositorioUsuario.buscarPorId(10L)).thenReturn(empleador);
    when(repositorioUsuario.buscarPorId(20L)).thenReturn(candidato);
    when(repositorioVacante.obtenerPorId(5L)).thenReturn(oferta);
    when(repositorioPostulacion.contarPorVacante(5L)).thenReturn(0L);
    when(repositorioPostulacion.buscarPorUsuarioYOferta(20L, "LOCAL:5")).thenReturn(null);
    when(repositorioInvitacion.buscarPorCandidatoYOferta(20L, 5L)).thenReturn(null);
    when(repositorioInvitacion.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    when(servicioPerfil.obtenerPerfil(20L)).thenReturn(new PerfilPostulante());
    when(servicioPerfil.obtenerHabilidades(20L)).thenReturn(List.of("Java"));
  }

  @Test
  void invitacionValidaGuardaLaInvitacionYNotificaConLaEmpresaYLaOferta() {
    Invitacion invitacion = servicio.invitar(10L, 20L, 5L);

    assertEquals(EstadoInvitacion.PENDIENTE, invitacion.getEstado());
    assertSameValues(empleador, candidato, oferta, invitacion);
    assertNotNull(invitacion.getFecha());
    verify(servicioNotificacionOferta)
      .notificar(
        candidato,
        "La empresa Empresa Ejemplo te invitó a postularte a Desarrollador Java",
        "/vacantes/detalle?fuente=LOCAL&id=5"
      );
  }

  @Test
  void noPermiteInvitarDosVecesAlMismoCandidatoALaMismaOferta() {
    when(repositorioInvitacion.buscarPorCandidatoYOferta(20L, 5L)).thenReturn(new Invitacion());

    assertThrows(IllegalArgumentException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
    verify(servicioNotificacionOferta, never()).notificar(any(), any(), any());
  }

  @Test
  void noPermiteInvitarAQuienYaSePostulo() {
    when(repositorioPostulacion.buscarPorUsuarioYOferta(20L, "LOCAL:5"))
      .thenReturn(new Postulacion());

    assertThrows(IllegalArgumentException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
  }

  @Test
  void noPermiteInvitarAUnaOfertaCerrada() {
    oferta.setEstado(EstadoVacante.CERRADA);

    assertThrows(OfertaCerradaException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
  }

  @Test
  void noPermiteInvitarCuandoElCupoEstaCompleto() {
    oferta.setLimitePostulaciones(1);
    when(repositorioPostulacion.contarPorVacante(5L)).thenReturn(1L);

    assertThrows(CupoPostulacionesCompletoException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
  }

  @Test
  void empleadorNoPuedeInvitarAUnaOfertaAjena() {
    oferta.setEmpleadorId(99L);

    assertThrows(IllegalArgumentException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
    verify(servicioNotificacionOferta, never()).notificar(any(), any(), any());
  }

  @Test
  void candidatoPuedeRechazarSoloSuInvitacionPendiente() {
    Invitacion invitacion = new Invitacion();
    invitacion.setEstado(EstadoInvitacion.PENDIENTE);
    when(repositorioInvitacion.buscarPorIdYCandidato(7L, 20L)).thenReturn(invitacion);

    servicio.rechazar(20L, 7L);

    assertEquals(EstadoInvitacion.RECHAZADA, invitacion.getEstado());
  }

  @Test
  void postularseDespuesDeLaInvitacionLaMarcaAceptada() {
    Invitacion invitacion = new Invitacion();
    invitacion.setEstado(EstadoInvitacion.PENDIENTE);
    when(repositorioInvitacion.buscarPorCandidatoYOferta(20L, 5L)).thenReturn(invitacion);

    servicio.aceptarSiExiste(20L, "LOCAL:5");

    assertEquals(EstadoInvitacion.ACEPTADA, invitacion.getEstado());
  }

  @Test
  void candidatosSugeridosEstanOrdenadosYRespetanLaVisibilidad() {
    Usuario candidatoConMasSkills = new Usuario();
    candidatoConMasSkills.setId(21L);
    candidatoConMasSkills.setRol("POSTULANTE");
    Usuario candidatoNoVisible = new Usuario();
    candidatoNoVisible.setId(22L);
    candidatoNoVisible.setRol("CANDIDATO");
    PerfilPostulante perfilNoVisible = new PerfilPostulante();
    perfilNoVisible.setVisibleParaEmpleadores(false);
    Skill sql = new Skill();
    sql.setNombre("SQL");
    VacanteSkill relacionSql = new VacanteSkill();
    relacionSql.setSkill(sql);
    oferta.setVacanteSkills(Set.of(oferta.getVacanteSkills().iterator().next(), relacionSql));
    when(repositorioUsuario.buscarPostulantes())
      .thenReturn(List.of(candidato, candidatoConMasSkills, candidatoNoVisible));
    when(servicioPerfil.obtenerPerfil(21L)).thenReturn(new PerfilPostulante());
    when(servicioPerfil.obtenerPerfil(22L)).thenReturn(perfilNoVisible);
    when(servicioPerfil.obtenerHabilidades(21L)).thenReturn(List.of("Java", "SQL"));
    when(servicioPerfil.obtenerHabilidades(22L)).thenReturn(List.of("Java"));
    when(repositorioInvitacion.buscarPorOferta(5L)).thenReturn(List.of());

    List<CandidatoSugeridoDTO> sugeridos = servicio.obtenerCandidatosSugeridos(5L, 10L);

    assertEquals(2, sugeridos.size());
    assertEquals(21L, sugeridos.get(0).getCandidatoId());
    assertEquals(20L, sugeridos.get(1).getCandidatoId());
  }

  @Test
  void noPermiteInvitarAUnCandidatoQueOcultoSuPerfil() {
    PerfilPostulante perfilNoVisible = new PerfilPostulante();
    perfilNoVisible.setVisibleParaEmpleadores(false);
    when(servicioPerfil.obtenerPerfil(20L)).thenReturn(perfilNoVisible);

    assertThrows(IllegalArgumentException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
  }

  private void assertSameValues(
    Usuario empleadorEsperado,
    Usuario candidatoEsperado,
    Vacante ofertaEsperada,
    Invitacion invitacion
  ) {
    assertEquals(empleadorEsperado, invitacion.getEmpleador());
    assertEquals(candidatoEsperado, invitacion.getCandidato());
    assertEquals(ofertaEsperada, invitacion.getOferta());
  }
}
