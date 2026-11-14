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
  void invitacionManualSiguePermitiendoUnaCoincidenciaDelCincuentaPorCiento() {
    Skill sql = new Skill();
    sql.setNombre("SQL");
    VacanteSkill relacionSql = new VacanteSkill();
    relacionSql.setSkill(sql);
    oferta.setVacanteSkills(Set.of(oferta.getVacanteSkills().iterator().next(), relacionSql));
    when(servicioPerfil.obtenerHabilidades(20L)).thenReturn(List.of("Java"));

    Invitacion invitacion = servicio.invitar(10L, 20L, 5L);

    assertEquals(EstadoInvitacion.PENDIENTE, invitacion.getEstado());
    verify(repositorioInvitacion).guardar(any());
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
  void candidatosConMenosDelCincuentaPorCientoNoSeSugieren() {
    Skill sql = new Skill();
    sql.setNombre("SQL");
    Skill git = new Skill();
    git.setNombre("Git");
    VacanteSkill relacionSql = new VacanteSkill();
    relacionSql.setSkill(sql);
    VacanteSkill relacionGit = new VacanteSkill();
    relacionGit.setSkill(git);
    oferta.setVacanteSkills(
      Set.of(oferta.getVacanteSkills().iterator().next(), relacionSql, relacionGit)
    );
    when(repositorioUsuario.buscarPostulantes()).thenReturn(List.of(candidato));
    when(repositorioInvitacion.buscarPorOferta(5L)).thenReturn(List.of());
    when(servicioPerfil.obtenerHabilidades(20L)).thenReturn(List.of("Java"));

    assertEquals(0, servicio.obtenerCandidatosSugeridos(5L, 10L).size());
  }

  @Test
  void invitacionMasivaAplicaLosFiltrosYOmitelosNoInvitables() {
    Skill sql = new Skill();
    sql.setNombre("SQL");
    VacanteSkill relacionSql = new VacanteSkill();
    relacionSql.setSkill(sql);
    relacionSql.setVacante(oferta);
    oferta.setVacanteSkills(Set.of(oferta.getVacanteSkills().iterator().next(), relacionSql));
    Usuario postulado = candidato(21L);
    Usuario yaInvitado = candidato(22L);
    Usuario noVisible = candidato(23L);
    Usuario bajoUmbral = candidato(24L);
    prepararCandidato(postulado, List.of("Java", "SQL"), null, new Postulacion());
    prepararCandidato(yaInvitado, List.of("Java", "SQL"), new Invitacion(), null);
    PerfilPostulante perfilOculto = new PerfilPostulante();
    perfilOculto.setVisibleParaEmpleadores(false);
    prepararCandidato(noVisible, List.of("Java", "SQL"), null, null);
    when(servicioPerfil.obtenerPerfil(23L)).thenReturn(perfilOculto);
    prepararCandidato(bajoUmbral, List.of("Java"), null, null);
    when(repositorioUsuario.buscarPostulantes())
      .thenReturn(List.of(candidato, postulado, yaInvitado, noVisible, bajoUmbral));
    when(repositorioVacante.obtenerPorIdParaActualizar(5L)).thenReturn(oferta);
    when(servicioPerfil.obtenerHabilidades(20L)).thenReturn(List.of("Java", "SQL"));

    ResumenInvitacionMasiva resumen = servicio.invitarMasivamente(10L, 5L, 75, List.of(" java "));

    assertEquals(1, resumen.getInvitados());
    assertEquals(3, resumen.getOmitidos());
    assertEquals(1, resumen.getOmitidosPorPostulacion());
    assertEquals(1, resumen.getOmitidosPorInvitacionExistente());
    assertEquals(1, resumen.getOmitidosPorVisibilidad());
    verify(repositorioInvitacion).guardar(any());
    verify(servicioNotificacionOferta).notificar(any(), any(), any());
  }

  @Test
  void invitacionMasivaInvitaTodosLosResultadosSinLimitarseALaPaginaVisible() {
    List<Usuario> candidatos = new java.util.ArrayList<>();
    candidatos.add(candidato);
    for (long id = 21; id <= 31; id++) {
      Usuario nuevo = candidato(id);
      candidatos.add(nuevo);
      prepararCandidato(nuevo, List.of("Java"), null, null);
    }
    when(repositorioUsuario.buscarPostulantes()).thenReturn(candidatos);
    when(repositorioVacante.obtenerPorIdParaActualizar(5L)).thenReturn(oferta);

    ResumenInvitacionMasiva resumen = servicio.invitarMasivamente(10L, 5L, 50, List.of());

    assertEquals(12, resumen.getInvitados());
    verify(repositorioInvitacion, org.mockito.Mockito.times(12)).guardar(any());
    verify(servicioNotificacionOferta, org.mockito.Mockito.times(12))
      .notificar(any(), any(), any());
  }

  @Test
  void invitacionMasivaRechazaOfertasCerradasLlenasYDeOtroEmpleador() {
    when(repositorioVacante.obtenerPorIdParaActualizar(5L)).thenReturn(oferta);

    oferta.setEstado(EstadoVacante.CERRADA);
    assertThrows(
      OfertaCerradaException.class,
      () -> servicio.invitarMasivamente(10L, 5L, 50, List.of())
    );

    oferta.setEstado(EstadoVacante.ABIERTA);
    oferta.setLimitePostulaciones(0);
    assertThrows(
      CupoPostulacionesCompletoException.class,
      () -> servicio.invitarMasivamente(10L, 5L, 50, List.of())
    );

    oferta.setLimitePostulaciones(3);
    oferta.setEmpleadorId(99L);
    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.invitarMasivamente(10L, 5L, 50, List.of())
    );
    verify(repositorioInvitacion, never()).guardar(any());
  }

  @Test
  void noPermiteInvitarAUnCandidatoQueOcultoSuPerfil() {
    PerfilPostulante perfilNoVisible = new PerfilPostulante();
    perfilNoVisible.setVisibleParaEmpleadores(false);
    when(servicioPerfil.obtenerPerfil(20L)).thenReturn(perfilNoVisible);

    assertThrows(IllegalArgumentException.class, () -> servicio.invitar(10L, 20L, 5L));

    verify(repositorioInvitacion, never()).guardar(any());
  }

  private Usuario candidato(Long id) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setRol("CANDIDATO");
    return usuario;
  }

  private void prepararCandidato(
    Usuario usuario,
    List<String> habilidades,
    Invitacion invitacion,
    Postulacion postulacion
  ) {
    when(servicioPerfil.obtenerPerfil(usuario.getId())).thenReturn(new PerfilPostulante());
    when(servicioPerfil.obtenerHabilidades(usuario.getId())).thenReturn(habilidades);
    when(repositorioPostulacion.buscarPorUsuarioYOferta(usuario.getId(), "LOCAL:5"))
      .thenReturn(postulacion);
    when(repositorioInvitacion.buscarPorCandidatoYOferta(usuario.getId(), 5L))
      .thenReturn(invitacion);
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
