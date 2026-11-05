package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServicioPostulantesOfertaTest {

  @Test
  void priorizaCandidatosPremiumYDespuesOrdenaPorFecha() {
    RepositorioPostulacion repositorioPostulacion = mock(RepositorioPostulacion.class);
    RepositorioVacante repositorioVacante = mock(RepositorioVacante.class);
    RepositorioCurriculumGenerado repositorioCurriculum = mock(RepositorioCurriculumGenerado.class);
    RepositorioPerfilPostulante repositorioPerfil = mock(RepositorioPerfilPostulante.class);
    ServicioPago servicioPago = mock(ServicioPago.class);
    Usuario premium = usuario(1L);
    Usuario gratuito = usuario(2L);
    Postulacion postulacionRecienteGratuita = postulacion(gratuito, LocalDateTime.now());
    Postulacion postulacionAnteriorPremium = postulacion(premium, LocalDateTime.now().minusDays(1));
    Vacante oferta = new Vacante();
    oferta.setId(10L);
    oferta.setEmpleadorId(42L);
    when(repositorioVacante.obtenerPorId(10L)).thenReturn(oferta);
    when(repositorioPostulacion.buscarPorVacante(10L))
      .thenReturn(List.of(postulacionRecienteGratuita, postulacionAnteriorPremium));
    when(repositorioCurriculum.buscarUsuarioIdsConCvOriginal(anyList())).thenReturn(List.of());
    when(servicioPago.esPremiumActivo(1L)).thenReturn(true);

    ServicioPostulantesOferta servicio = new ServicioPostulantesOferta(
      repositorioPostulacion,
      repositorioVacante,
      repositorioCurriculum,
      repositorioPerfil,
      servicioPago
    );

    List<PostulanteOfertaDTO> resultado = servicio.obtenerPostulantes(10L, 42L);

    assertEquals(1L, resultado.getFirst().getPostulacionId());
    assertTrue(resultado.getFirst().isPremium());
    assertEquals(2L, resultado.getLast().getPostulacionId());
    assertEquals(false, resultado.getLast().isPremium());
  }

  @Test
  void rechazaConsultarPostulantesDeUnaOfertaAjena() {
    RepositorioPostulacion repositorioPostulacion = mock(RepositorioPostulacion.class);
    RepositorioVacante repositorioVacante = mock(RepositorioVacante.class);
    RepositorioCurriculumGenerado repositorioCurriculum = mock(RepositorioCurriculumGenerado.class);
    RepositorioPerfilPostulante repositorioPerfil = mock(RepositorioPerfilPostulante.class);
    Vacante oferta = new Vacante();
    oferta.setEmpleadorId(99L);
    when(repositorioVacante.obtenerPorId(10L)).thenReturn(oferta);
    ServicioPostulantesOferta servicio = new ServicioPostulantesOferta(
      repositorioPostulacion,
      repositorioVacante,
      repositorioCurriculum,
      repositorioPerfil
    );

    assertThrows(AccesoOfertaDenegadoException.class, () -> servicio.obtenerPostulantes(10L, 42L));
  }

  @Test
  void cambiarEstadoRegistraFechaDeDecisionSoloCuandoCambiaElEstado() {
    RepositorioPostulacion repositorioPostulacion = mock(RepositorioPostulacion.class);
    RepositorioVacante repositorioVacante = mock(RepositorioVacante.class);
    Postulacion postulacion = new Postulacion();
    postulacion.setId(20L);
    postulacion.setEstado(EstadoPostulacion.PENDIENTE);
    Vacante oferta = new Vacante();
    oferta.setEmpleadorId(42L);
    when(repositorioVacante.obtenerPorId(10L)).thenReturn(oferta);
    when(repositorioPostulacion.buscarPorIdYVacante(20L, 10L)).thenReturn(postulacion);
    ServicioPostulantesOferta servicio = new ServicioPostulantesOferta(
      repositorioPostulacion,
      repositorioVacante,
      mock(RepositorioCurriculumGenerado.class),
      mock(RepositorioPerfilPostulante.class)
    );

    servicio.cambiarEstado(10L, 20L, 42L, EstadoPostulacion.ACEPTADA);
    LocalDateTime fechaDecision = postulacion.getFechaDecision();
    servicio.cambiarEstado(10L, 20L, 42L, EstadoPostulacion.ACEPTADA);

    assertNotNull(fechaDecision);
    assertEquals(fechaDecision, postulacion.getFechaDecision());
    assertThrows(IllegalArgumentException.class, () -> servicio.cambiarEstado(10L, 20L, 42L, null));
  }

  @Test
  void curriculumAusenteDevuelveNullYPerfilUsaDatosDelUsuario() {
    RepositorioPostulacion repositorioPostulacion = mock(RepositorioPostulacion.class);
    RepositorioVacante repositorioVacante = mock(RepositorioVacante.class);
    RepositorioCurriculumGenerado repositorioCurriculum = mock(RepositorioCurriculumGenerado.class);
    RepositorioPerfilPostulante repositorioPerfil = mock(RepositorioPerfilPostulante.class);
    Usuario usuario = usuario(1L);
    usuario.setNombre("Ada");
    usuario.setApellido("Lovelace");
    usuario.setEmail("ada@example.com");
    usuario.setTelefono("123456");
    usuario.setUbicacion("Buenos Aires");
    usuario.setHabilidades(List.of("Java, SQL", "Java"));
    Postulacion postulacion = postulacion(usuario, LocalDateTime.now());
    postulacion.setId(20L);
    Vacante oferta = new Vacante();
    oferta.setId(10L);
    oferta.setEmpleadorId(42L);
    oferta.setTitulo("Desarrolladora");
    when(repositorioVacante.obtenerPorId(10L)).thenReturn(oferta);
    when(repositorioPostulacion.buscarPorIdYVacante(20L, 10L)).thenReturn(postulacion);
    when(repositorioCurriculum.buscarPorUsuarioId(1L)).thenReturn(null);
    when(repositorioPerfil.buscarPorUsuarioIdParaLectura(1L)).thenReturn(null);
    ServicioPostulantesOferta servicio = new ServicioPostulantesOferta(
      repositorioPostulacion,
      repositorioVacante,
      repositorioCurriculum,
      repositorioPerfil
    );

    assertNull(servicio.obtenerCvPostulante(10L, 20L, 42L));
    PerfilPostulanteDTO perfil = servicio.obtenerPerfilPostulante(10L, 20L, 42L);

    assertEquals("Ada Lovelace", perfil.getNombreCompleto());
    assertEquals(List.of("Java", "SQL"), perfil.getHabilidades());
    assertEquals("123456", perfil.getTelefono());
    assertEquals("Buenos Aires", perfil.getUbicacion());
    verify(repositorioPerfil).buscarPorUsuarioIdParaLectura(1L);
  }

  private Usuario usuario(Long id) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setRol("CANDIDATO");
    usuario.setNombre("Candidato");
    return usuario;
  }

  private Postulacion postulacion(Usuario usuario, LocalDateTime fecha) {
    Postulacion postulacion = new Postulacion();
    postulacion.setId(usuario.getId());
    postulacion.setUsuario(usuario);
    postulacion.setFechaPostulacion(fecha);
    return postulacion;
  }
}
