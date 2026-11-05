package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import com.tallerwebi.dominio.excepcion.MensajeInvalido;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioMensajeImplTest {

  private RepositorioMensaje repositorioMensaje;
  private RepositorioPostulacion repositorioPostulacion;
  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private ServicioMensajeImpl servicio;
  private Usuario postulante;
  private Usuario empleador;
  private Postulacion postulacion;
  private Vacante vacante;

  @BeforeEach
  void iniciar() {
    repositorioMensaje = mock(RepositorioMensaje.class);
    repositorioPostulacion = mock(RepositorioPostulacion.class);
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    servicio =
      new ServicioMensajeImpl(
        repositorioMensaje,
        repositorioPostulacion,
        repositorioUsuario,
        repositorioVacante
      );
    postulante = usuario(7L);
    empleador = usuario(8L);
    empleador.setEmpresa("Empresa Sur");
    vacante = new Vacante();
    vacante.setId(33L);
    vacante.setEmpleadorId(8L);
    postulacion = new Postulacion();
    postulacion.setId(52L);
    postulacion.setIdOferta("LOCAL:33");
    postulacion.setUsuario(postulante);
    when(repositorioPostulacion.buscarPorId(52L)).thenReturn(postulacion);
    when(repositorioVacante.obtenerPorId(33L)).thenReturn(vacante);
    when(repositorioUsuario.buscarPorId(7L)).thenReturn(postulante);
    when(repositorioUsuario.buscarPorId(8L)).thenReturn(empleador);
    when(repositorioMensaje.guardarMensaje(any(Mensaje.class)))
      .thenAnswer(invocacion -> invocacion.getArgument(0));
  }

  @Test
  void postulanteEnviaTextoRecortadoAlEmpleador() throws Exception {
    Mensaje mensaje = servicio.enviar(52L, 7L, "  Hola  ");

    assertEquals("Hola", mensaje.getTexto());
    assertSame(postulante, mensaje.getEmisor());
    assertSame(empleador, mensaje.getReceptor());
    assertSame(postulacion, mensaje.getPostulacion());
    verify(repositorioMensaje).guardarMensaje(mensaje);
  }

  @Test
  void empleadorEnviaMensajeAlPostulante() throws Exception {
    Mensaje mensaje = servicio.enviar(52L, 8L, "Respuesta");

    assertSame(empleador, mensaje.getEmisor());
    assertSame(postulante, mensaje.getReceptor());
    assertEquals("Respuesta", mensaje.getTexto());
  }

  @Test
  void textoNuloOVacioSeRechazaAntesDeConsultarLaPostulacion() {
    assertThrows(MensajeInvalido.class, () -> servicio.enviar(52L, 7L, "  "));
    assertThrows(MensajeInvalido.class, () -> servicio.enviar(52L, 7L, null));

    verify(repositorioPostulacion, org.mockito.Mockito.never()).buscarPorId(any());
  }

  @Test
  void mensajeMayorAUnMillarDeCaracteresSeRechaza() {
    assertThrows(MensajeInvalido.class, () -> servicio.enviar(52L, 7L, "x".repeat(1001)));

    verify(repositorioMensaje, org.mockito.Mockito.never()).guardarMensaje(any());
  }

  @Test
  void postulacionInexistenteDeniegaElEnvio() {
    when(repositorioPostulacion.buscarPorId(99L)).thenReturn(null);

    assertThrows(AccesoChatDenegado.class, () -> servicio.enviar(99L, 7L, "Hola"));
  }

  @Test
  void usuarioAjenoNoPuedeListarLaConversacion() {
    assertThrows(AccesoChatDenegado.class, () -> servicio.listar(52L, 99L));

    verify(repositorioMensaje, org.mockito.Mockito.never()).marcarLeidos(52L, 99L);
  }

  @Test
  void listarMarcaComoLeidosYDevuelveLosMensajesDeLaPostulacion() throws Exception {
    List<Mensaje> mensajes = List.of(new Mensaje());
    when(repositorioMensaje.listarPorPostulacion(52L)).thenReturn(mensajes);

    List<Mensaje> resultado = servicio.listar(52L, 8L);

    assertSame(mensajes, resultado);
    verify(repositorioMensaje).marcarLeidos(52L, 8L);
  }

  @Test
  void notificacionesUsanEmpresaComoNombreDelEmisor() {
    Mensaje mensaje = mensajeConEmisor(empleador);
    when(repositorioMensaje.listarNoLeidos(7L)).thenReturn(List.of(mensaje));

    List<NotificacionMensaje> resultado = servicio.listarNoLeidos(7L);

    assertEquals(1, resultado.size());
    assertEquals("Empresa Sur", resultado.getFirst().getEmisorNombre());
    assertEquals("Mensaje de prueba", resultado.getFirst().getTexto());
    assertEquals(52L, resultado.getFirst().getPostulacionId());
  }

  @Test
  void notificacionesComponenElNombrePersonalYPermitenEmisorSinNombre() {
    postulante.setNombre("Ada");
    postulante.setApellido("Lovelace");
    Mensaje conNombre = mensajeConEmisor(postulante);
    Mensaje sinNombre = mensajeConEmisor(usuario(9L));
    when(repositorioMensaje.listarNoLeidos(8L)).thenReturn(List.of(conNombre, sinNombre));

    List<NotificacionMensaje> resultado = servicio.listarNoLeidos(8L);

    assertEquals("Ada Lovelace", resultado.get(0).getEmisorNombre());
    assertEquals("", resultado.get(1).getEmisorNombre());
    assertEquals("Mensaje de prueba", resultado.get(1).getTexto());
  }

  private Usuario usuario(Long id) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setEmail(id + "@example.com");
    return usuario;
  }

  private Mensaje mensajeConEmisor(Usuario emisor) {
    Mensaje mensaje = new Mensaje();
    mensaje.setPostulacion(postulacion);
    mensaje.setEmisor(emisor);
    mensaje.setTexto("Mensaje de prueba");
    return mensaje;
  }
}
