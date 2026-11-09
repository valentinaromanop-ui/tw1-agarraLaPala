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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioResenaImplTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private RepositorioPostulacion repositorioPostulacion;
  private RepositorioResena repositorioResena;
  private ServicioResenaImpl servicio;
  private Usuario candidato;
  private Usuario empleador;
  private Vacante oferta;

  @BeforeEach
  void iniciar() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    repositorioPostulacion = mock(RepositorioPostulacion.class);
    repositorioResena = mock(RepositorioResena.class);
    servicio =
      new ServicioResenaImpl(
        repositorioUsuario,
        repositorioVacante,
        repositorioPostulacion,
        repositorioResena
      );

    candidato = usuario(1L, "CANDIDATO");
    empleador = usuario(2L, "EMPLEADOR");
    oferta = new Vacante();
    oferta.setId(42L);
    oferta.setEmpleadorId(empleador.getId());

    when(repositorioUsuario.buscarPorId(1L)).thenReturn(candidato);
    when(repositorioUsuario.buscarPorId(2L)).thenReturn(empleador);
    when(repositorioVacante.buscarPorEmpleador(2L)).thenReturn(List.of(oferta));
    when(repositorioResena.guardar(any(Resena.class)))
      .thenAnswer(invocacion -> invocacion.getArgument(0));
  }

  @Test
  void noPermiteResenarSinHabersePostuladoAUnaOfertaDeLaEmpresa() {
    when(repositorioPostulacion.buscarPorUsuarioId(1L)).thenReturn(List.of());

    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.crear(1L, 2L, 5, "Buena experiencia")
    );

    verify(repositorioResena, never()).guardar(any(Resena.class));
  }

  @Test
  void creaUnaResenaConElCandidatoYLaEmpresaCorrectos() {
    when(repositorioPostulacion.buscarPorUsuarioId(1L))
      .thenReturn(List.of(postulacion(1L, "LOCAL:42")));

    Resena resultado = servicio.crear(1L, 2L, 4, "  Buena experiencia  ");

    assertEquals(candidato, resultado.getCandidato());
    assertEquals(empleador, resultado.getEmpleador());
    assertEquals(4, resultado.getPuntaje());
    assertEquals("Buena experiencia", resultado.getComentario());
    assertNotNull(resultado.getFecha());
    verify(repositorioResena).guardar(resultado);
  }

  @Test
  void noPermiteUnaSegundaResenaDelMismoCandidato() {
    when(repositorioPostulacion.buscarPorUsuarioId(1L))
      .thenReturn(List.of(postulacion(1L, "LOCAL:42")));
    when(repositorioResena.buscarPorCandidatoYEmpleador(1L, 2L)).thenReturn(new Resena());

    assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 2L, 4, "Otra opinión"));

    verify(repositorioResena, never()).guardar(any(Resena.class));
  }

  @Test
  void calculaPromedioYCantidadDeResenas() {
    Resena primera = resena(1L, 5);
    Resena segunda = resena(1L, 4);
    Resena tercera = resena(1L, 2);
    when(repositorioResena.buscarPorEmpleador(2L)).thenReturn(List.of(primera, segunda, tercera));

    ResumenResenas resumen = servicio.obtenerResumen(2L);

    assertEquals(2L, resumen.getEmpleadorId());
    assertEquals(3L, resumen.getCantidad());
    assertEquals("3.7", resumen.getPromedio().toPlainString());
  }

  @Test
  void soloElAutorPuedeEditarOBorrarSuResena() {
    Resena propia = resena(1L, 3);
    propia.setId(10L);
    Resena ajena = resena(3L, 4);
    ajena.setId(11L);
    when(repositorioResena.buscarPorId(10L)).thenReturn(propia);
    when(repositorioResena.buscarPorId(11L)).thenReturn(ajena);

    servicio.editar(1L, 2L, 10L, 5, "Actualicé mi opinión");
    assertEquals(5, propia.getPuntaje());
    assertEquals("Actualicé mi opinión", propia.getComentario());
    verify(repositorioResena).guardar(propia);

    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.editar(1L, 2L, 11L, 2, "Intento ajeno")
    );
    assertThrows(IllegalArgumentException.class, () -> servicio.eliminar(1L, 2L, 11L));
    verify(repositorioResena, never()).eliminar(ajena);

    Resena propiaParaEliminar = resena(1L, 3);
    propiaParaEliminar.setId(12L);
    when(repositorioResena.buscarPorId(12L)).thenReturn(propiaParaEliminar);
    servicio.eliminar(1L, 2L, 12L);
    verify(repositorioResena).eliminar(propiaParaEliminar);
  }

  @Test
  void soloElEmpleadorPuedeResponderYUnaVez() {
    Resena resena = resena(1L, 5);
    resena.setId(10L);
    when(repositorioResena.buscarPorIdParaActualizar(10L)).thenReturn(resena);

    servicio.responder(2L, 10L, "Gracias por tu comentario");

    assertEquals("Gracias por tu comentario", resena.getRespuestaEmpleador());
    assertNotNull(resena.getFechaRespuesta());
    verify(repositorioResena).guardar(resena);
    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.responder(2L, 10L, "Otra respuesta")
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.responder(1L, 10L, "Respuesta de candidato")
    );
  }

  @Test
  void validaPuntajeComentarioYRolAntesDeGuardar() {
    when(repositorioPostulacion.buscarPorUsuarioId(1L)).thenReturn(List.of(postulacion(1L, "42")));

    assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 2L, 0, "Comentario"));
    assertThrows(IllegalArgumentException.class, () -> servicio.crear(1L, 2L, 4, "  "));
    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.crear(2L, 2L, 4, "Un empleador no reseña")
    );
    verify(repositorioResena, never()).guardar(any(Resena.class));
  }

  private Usuario usuario(Long id, String rol) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setRol(rol);
    return usuario;
  }

  private Postulacion postulacion(Long candidatoId, String idOferta) {
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(usuario(candidatoId, "CANDIDATO"));
    postulacion.setIdOferta(idOferta);
    return postulacion;
  }

  private Resena resena(Long candidatoId, int puntaje) {
    Resena resena = new Resena();
    resena.setCandidato(usuario(candidatoId, "CANDIDATO"));
    resena.setEmpleador(empleador);
    resena.setPuntaje(puntaje);
    resena.setComentario("Comentario de prueba");
    return resena;
  }
}
