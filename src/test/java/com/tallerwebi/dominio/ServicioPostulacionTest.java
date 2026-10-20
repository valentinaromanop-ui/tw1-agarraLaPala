package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPostulacionTest {

  private RepositorioUsuario usuarios;
  private RepositorioVacante vacantes;
  private RepositorioPostulacion postulaciones;
  private RepositorioCurriculumGenerado curriculums;
  private RepositorioPerfilPostulante perfiles;
  private ServicioPostulacion servicio;
  private VacanteDTO oferta;

  @BeforeEach
  public void init() {
    usuarios = mock(RepositorioUsuario.class);
    vacantes = mock(RepositorioVacante.class);
    postulaciones = mock(RepositorioPostulacion.class);
    curriculums = mock(RepositorioCurriculumGenerado.class);
    perfiles = mock(RepositorioPerfilPostulante.class);
    servicio =
      new ServicioPostulacionImpl(
        usuarios,
        postulaciones,
        vacantes,
        new ServicioPostulantesOferta(postulaciones, vacantes, curriculums, perfiles)
      );
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
    assertEquals(EstadoPostulacion.ENVIADA, resultado.getEstado());
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
  public void deberiaGuardarUnSnapshotYRechazarLaDuplicada() {
    Postulacion entrada = new Postulacion();
    entrada.setIdOferta("JOBICY:10");
    entrada.setTitulo("Desarrollador Java");
    entrada.setEmpresa("Empresa A");

    Postulacion resultado = servicio.registrarPostulacion(1L, entrada);

    assertEquals(EstadoPostulacion.ENVIADA, resultado.getEstado());
    assertEquals("Empresa A", resultado.getEmpresa());
    when(postulaciones.buscarPorUsuarioYOferta(1L, "JOBICY:10")).thenReturn(resultado);

    Postulacion duplicada = new Postulacion();
    duplicada.setIdOferta("JOBICY:10");
    IllegalArgumentException excepcion = assertThrows(
      IllegalArgumentException.class,
      () -> servicio.registrarPostulacion(1L, duplicada)
    );
    assertEquals("Ya te postulaste a esta oferta", excepcion.getMessage());
    verify(postulaciones).guardar(entrada);
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

  @Test
  public void noDeberiaPostularseAUnaVacanteLocalCerrada() {
    oferta.setFuente("LOCAL");
    Vacante local = new Vacante();
    local.setEstado(EstadoVacante.CERRADA);
    when(vacantes.obtenerPorId(10L)).thenReturn(local);

    assertThrows(OfertaCerradaException.class, () -> servicio.registrarEnVacante(1L, oferta));
    verify(postulaciones, never()).guardar(any());
  }

  @Test
  public void noDeberiaSuperarElLimiteDePostulacionesDeUnaVacante() {
    oferta.setFuente("LOCAL");
    Vacante local = new Vacante();
    local.setId(10L);
    local.setEmpleadorId(42L);
    local.setLimitePostulaciones(1);
    when(vacantes.obtenerPorId(10L)).thenReturn(local);
    when(postulaciones.contarPorVacante(10L)).thenReturn(1L);

    assertThrows(
      CupoPostulacionesCompletoException.class,
      () -> servicio.registrarEnVacante(1L, oferta)
    );
    verify(postulaciones, never()).guardar(any());
  }

  @Test
  public void empleadorNoPuedeVerPostulantesDeOfertaAjena() {
    Vacante vacanteAjena = ofertaDeEmpleador(10L, 99L);
    when(vacantes.obtenerPorId(10L)).thenReturn(vacanteAjena);

    assertThrows(AccesoOfertaDenegadoException.class, () -> servicio.obtenerPostulantes(10L, 42L));
    verify(postulaciones, never()).buscarPorVacante(10L);
  }

  @Test
  public void noDeberiaObtenerCvDeUnaPostulacionDeOtraOferta() {
    Vacante ofertaPropia = ofertaDeEmpleador(10L, 42L);
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaPropia);

    assertThrows(
      VacanteNoEncontradaException.class,
      () -> servicio.obtenerCvPostulante(10L, 20L, 42L)
    );
    verify(curriculums, never()).buscarPorUsuarioId(any());
  }

  @Test
  public void deberiaIncluirNombreHabilidadesYDisponibilidadDelCv() {
    Vacante ofertaPropia = ofertaDeEmpleador(10L, 42L);
    Usuario candidato = new Usuario();
    candidato.setId(7L);
    candidato.setNombre("Ada");
    candidato.setApellido("Lovelace");
    candidato.setEmail("ada@example.com");
    candidato.setHabilidades(java.util.List.of("Java", "SQL"));
    Postulacion postulacion = new Postulacion();
    postulacion.setId(20L);
    postulacion.setUsuario(candidato);
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaPropia);
    when(postulaciones.buscarPorVacante(10L)).thenReturn(java.util.List.of(postulacion));
    when(curriculums.buscarUsuarioIdsConCvOriginal(java.util.List.of(7L)))
      .thenReturn(java.util.List.of(7L));

    PostulanteOfertaDTO resultado = servicio.obtenerPostulantes(10L, 42L).get(0);

    assertEquals(20L, resultado.getPostulacionId());
    assertEquals("Ada Lovelace", resultado.getNombreCompleto());
    assertEquals("ada@example.com", resultado.getEmail());
    assertEquals(java.util.List.of("Java", "SQL"), resultado.getHabilidades());
    assertTrue(resultado.isTieneCv());
    verify(curriculums).buscarUsuarioIdsConCvOriginal(java.util.List.of(7L));
  }

  @Test
  public void deberiaPermitirObtenerElCvDePostulacionDeOfertaPropia() {
    Vacante ofertaPropia = ofertaDeEmpleador(10L, 42L);
    Usuario candidato = new Usuario();
    candidato.setId(7L);
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(candidato);
    CurriculumGenerado curriculum = new CurriculumGenerado();
    curriculum.setNombreArchivoOriginal("cv.pdf");
    curriculum.setArchivoOriginal(new byte[] { 1, 2, 3 });
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaPropia);
    when(postulaciones.buscarPorIdYVacante(20L, 10L)).thenReturn(postulacion);
    when(curriculums.buscarPorUsuarioId(7L)).thenReturn(curriculum);

    ArchivoCvPostulanteDTO resultado = servicio.obtenerCvPostulante(10L, 20L, 42L);

    assertEquals("cv.pdf", resultado.getNombreArchivo());
    assertArrayEquals(new byte[] { 1, 2, 3 }, resultado.getContenido());
  }

  @Test
  public void deberiaProyectarPerfilPublicableSinExponerLaEntidadUsuario() {
    Vacante ofertaPropia = ofertaDeEmpleador(10L, 42L);
    ofertaPropia.setTitulo("Desarrolladora Java");
    Usuario candidato = new Usuario();
    candidato.setId(7L);
    candidato.setNombre("Ada");
    candidato.setApellido("Lovelace");
    candidato.setEmail("ada@example.com");
    candidato.setPassword("no-debe-exponerse");
    candidato.setFotoPerfil(new byte[] { 4, 5 });
    candidato.setFotoPerfilTipo("image/png");
    candidato.setHabilidades(java.util.List.of("Java, SQL"));
    Postulacion postulacion = new Postulacion();
    postulacion.setId(20L);
    postulacion.setUsuario(candidato);
    postulacion.setFechaPostulacion(java.time.LocalDateTime.of(2026, 10, 20, 15, 30));
    PerfilPostulante perfil = new PerfilPostulante();
    perfil.setPresentacion("Desarrollo software");
    perfil.setLinkedin("https://example.com/ada");
    perfil.setGithub("javascript:alert(1)");
    perfil.setDisponibilidadHoraria(java.util.List.of("Mañana"));
    perfil.setIdiomas(java.util.Map.of("Inglés", "Avanzado"));
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaPropia);
    when(postulaciones.buscarPorIdYVacante(20L, 10L)).thenReturn(postulacion);
    when(perfiles.buscarPorUsuarioIdParaLectura(7L)).thenReturn(perfil);

    PerfilPostulanteDTO resultado = servicio.obtenerPerfilPostulante(10L, 20L, 42L);

    assertEquals("Ada Lovelace", resultado.getNombreCompleto());
    assertEquals("ada@example.com", resultado.getEmail());
    assertEquals("AL", resultado.getIniciales());
    assertTrue(resultado.isTieneFoto());
    assertEquals(java.util.List.of("Java", "SQL"), resultado.getHabilidades());
    assertEquals("Desarrollo software", resultado.getPresentacion());
    assertEquals(java.util.List.of("Mañana"), resultado.getDisponibilidadHoraria());
    assertEquals(java.util.Map.of("Inglés", "Avanzado"), resultado.getIdiomas());
    assertEquals("https://example.com/ada", resultado.getLinkedin());
    assertNull(resultado.getGithub());
    assertEquals("Desarrolladora Java", resultado.getTituloOferta());
    assertEquals(java.time.LocalDateTime.of(2026, 10, 20, 15, 30), resultado.getFechaPostulacion());
    org.junit.jupiter.api.Assertions.assertFalse(
      java.util.Arrays
        .stream(resultado.getClass().getMethods())
        .anyMatch(metodo -> metodo.getName().equals("getPassword"))
    );
  }

  @Test
  public void noDeberiaConsultarPerfilSiOfertaEsAjena() {
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaDeEmpleador(10L, 99L));

    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicio.obtenerPerfilPostulante(10L, 20L, 42L)
    );

    verify(postulaciones, never()).buscarPorIdYVacante(any(), any());
    verify(perfiles, never()).buscarPorUsuarioIdParaLectura(any());
  }

  @Test
  public void noDeberiaConsultarPostulanteSiNoPerteneceALaOferta() {
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaDeEmpleador(10L, 42L));
    when(postulaciones.buscarPorIdYVacante(20L, 10L)).thenReturn(null);

    assertThrows(
      VacanteNoEncontradaException.class,
      () -> servicio.obtenerPerfilPostulante(10L, 20L, 42L)
    );

    verify(perfiles, never()).buscarPorUsuarioIdParaLectura(any());
    verify(curriculums, never()).buscarPorUsuarioId(any());
  }

  @Test
  public void deberiaObtenerLaFotoSoloTrasValidarLaOfertaYPostulacion() {
    Vacante ofertaPropia = ofertaDeEmpleador(10L, 42L);
    Usuario candidato = new Usuario();
    candidato.setId(7L);
    candidato.setFotoPerfil(new byte[] { 4, 5 });
    candidato.setFotoPerfilTipo("image/png");
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(candidato);
    when(vacantes.obtenerPorId(10L)).thenReturn(ofertaPropia);
    when(postulaciones.buscarPorIdYVacante(20L, 10L)).thenReturn(postulacion);

    FotoPerfilPostulanteDTO resultado = servicio.obtenerFotoPerfilPostulante(10L, 20L, 42L);

    assertEquals("image/png", resultado.getContentType());
    assertArrayEquals(new byte[] { 4, 5 }, resultado.getContenido());
  }

  private Vacante ofertaDeEmpleador(Long id, Long empleadorId) {
    Vacante vacante = new Vacante();
    vacante.setId(id);
    vacante.setEmpleadorId(empleadorId);
    return vacante;
  }
}
