package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ServicioChatTest {

  private RepositorioPostulacion repositorioPostulacion;
  private RepositorioMensaje repositorioMensaje;
  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private ServicioChatImpl servicioChat;
  private Postulacion postulacion;
  private Usuario postulante;
  private Usuario empleador;
  private Vacante vacante;

  @BeforeEach
  void setUp() {
    repositorioPostulacion = mock(RepositorioPostulacion.class);
    repositorioMensaje = mock(RepositorioMensaje.class);
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    servicioChat =
      new ServicioChatImpl(
        repositorioPostulacion,
        repositorioMensaje,
        repositorioUsuario,
        repositorioVacante
      );

    postulante = usuario(7L);
    empleador = usuario(8L);
    vacante = new Vacante();
    vacante.setId(33L);
    vacante.setEmpleadorId(8L);
    postulacion = new Postulacion();
    postulacion.setId(52L);
    postulacion.setIdOferta("LOCAL:33");
    postulacion.setUsuario(postulante);
    postulacion.setEstado(EstadoPostulacion.ACEPTADA);
    postulacion.setFechaPostulacion(LocalDateTime.of(2026, 11, 4, 18, 30));

    when(repositorioPostulacion.buscarPorId(52L)).thenReturn(postulacion);
    when(repositorioVacante.obtenerPorId(33L)).thenReturn(vacante);
    when(repositorioVacante.buscarPorEmpleador(any())).thenReturn(List.of());
  }

  @Test
  void mensajeEnviadoPorPostulanteEsVisibleParaEmpleador() {
    Mensaje mensaje = new Mensaje();
    mensaje.setId(1L);
    mensaje.setPostulacion(postulacion);
    mensaje.setRemitente(postulante);
    mensaje.setContenido("Hola");
    mensaje.setFechaEnvio(LocalDateTime.of(2026, 11, 4, 19, 0));
    when(repositorioUsuario.buscarPorId(7L)).thenReturn(postulante);
    when(repositorioMensaje.guardar(any(Mensaje.class))).thenReturn(mensaje);
    when(repositorioMensaje.listarPorPostulacion(52L)).thenReturn(List.of(mensaje));

    Mensaje enviado = servicioChat.enviar(7L, 52L, "Hola");
    List<Mensaje> visibles = servicioChat.listarMensajes(8L, 52L);

    assertThat(enviado.getContenido(), equalTo("Hola"));
    assertThat(visibles, equalTo(List.of(mensaje)));
    verify(repositorioMensaje).guardar(any(Mensaje.class));
  }

  @Test
  void conversacionAceptadaSinMensajesDevuelveListaVacia() {
    when(repositorioMensaje.listarPorPostulacion(52L)).thenReturn(List.of());

    assertThat(servicioChat.listarMensajes(7L, 52L), equalTo(List.of()));
  }

  @Test
  void postulacionInexistenteDevuelve404() {
    when(repositorioPostulacion.buscarPorId(999L)).thenReturn(null);

    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> servicioChat.listarMensajes(7L, 999L)
    );

    assertThat(excepcion.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void usuarioQueNoParticipaDevuelve403() {
    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> servicioChat.listarMensajes(99L, 52L)
    );

    assertThat(excepcion.getStatusCode(), equalTo(HttpStatus.FORBIDDEN));
  }

  @Test
  void mensajeVacioDevuelve400() {
    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> servicioChat.enviar(7L, 52L, "  ")
    );

    assertThat(excepcion.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void mensajeDemasiadoLargoDevuelve400SinGuardar() {
    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> servicioChat.enviar(7L, 52L, "x".repeat(2001))
    );

    assertThat(excepcion.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    verify(repositorioMensaje, never()).guardar(any(Mensaje.class));
  }

  @Test
  void usuarioDeSesionInexistenteNoPuedeEnviar() {
    when(repositorioUsuario.buscarPorId(7L)).thenReturn(null);

    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> servicioChat.enviar(7L, 52L, "Hola")
    );

    assertThat(excepcion.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    verify(repositorioMensaje, never()).guardar(any(Mensaje.class));
  }

  @Test
  void listarConversacionesCombinaPostulacionesYVacantesDelEmpleadorYOrdenaPorUltimoMensaje() {
    Postulacion masNueva = postulacion(54L, "LOCAL:33", EstadoPostulacion.ACEPTADA);
    Postulacion masVieja = postulacion(53L, "LOCAL:33", EstadoPostulacion.ACEPTADA);
    when(repositorioPostulacion.buscarPorUsuarioId(8L)).thenReturn(List.of());
    when(repositorioVacante.buscarPorEmpleador(8L)).thenReturn(List.of(vacante));
    when(repositorioPostulacion.buscarPorVacante(33L)).thenReturn(List.of(masVieja, masNueva));
    when(repositorioMensaje.listarPorPostulacion(53L)).thenReturn(List.of());
    when(repositorioMensaje.listarPorPostulacion(54L))
      .thenReturn(List.of(mensajeConFecha(54L, LocalDateTime.of(2026, 11, 5, 10, 0))));

    List<Postulacion> resultado = servicioChat.listarConversaciones(8L);

    assertThat(resultado, equalTo(List.of(masNueva, masVieja)));
  }

  @Test
  void listarConversacionesOmiteEstadosNoAceptadosYParticipantesAjeno() {
    Postulacion pendiente = postulacion(53L, "LOCAL:33", EstadoPostulacion.PENDIENTE);
    Postulacion ajena = postulacion(54L, "JOBICY:9", EstadoPostulacion.ACEPTADA);
    ajena.setUsuario(usuario(99L));
    when(repositorioPostulacion.buscarPorUsuarioId(7L)).thenReturn(List.of(pendiente, ajena));
    when(repositorioVacante.buscarPorEmpleador(7L)).thenReturn(List.of());

    List<Postulacion> resultado = servicioChat.listarConversaciones(7L);

    assertThat(resultado, equalTo(List.of()));
  }

  @Test
  void contarNoLeidosConsultaCadaConversacionAceptada() {
    when(repositorioPostulacion.buscarPorUsuarioId(7L)).thenReturn(List.of(postulacion));
    when(repositorioMensaje.listarPorPostulacion(52L)).thenReturn(List.of());
    when(repositorioMensaje.contarNoLeidos(52L, 7L)).thenReturn(3L);

    long cantidad = servicioChat.contarNoLeidos(7L);

    assertThat(cantidad, equalTo(3L));
    verify(repositorioMensaje).contarNoLeidos(52L, 7L);
  }

  private Usuario usuario(Long id) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setEmail(id + "@example.com");
    return usuario;
  }

  private Postulacion postulacion(Long id, String ofertaId, EstadoPostulacion estado) {
    Postulacion postulacion = new Postulacion();
    postulacion.setId(id);
    postulacion.setIdOferta(ofertaId);
    postulacion.setUsuario(postulante);
    postulacion.setEstado(estado);
    postulacion.setFechaPostulacion(LocalDateTime.of(2026, 11, 1, 10, 0));
    return postulacion;
  }

  private Mensaje mensajeConFecha(Long postulacionId, LocalDateTime fecha) {
    Mensaje mensaje = new Mensaje();
    mensaje.setPostulacion(postulacion);
    mensaje.setFechaEnvio(fecha);
    return mensaje;
  }
}
