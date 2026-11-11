package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioMensajeTest {

  private RepositorioMensaje repositorioMensaje;
  private RepositorioPostulacion repositorioPostulacion;
  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private ServicioMensajeImpl servicio;
  private Postulacion postulacion;
  private Usuario candidato;
  private Usuario empleador;

  @BeforeEach
  public void init() {
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
    candidato = usuario(7L, "Ana");
    empleador = usuario(9L, "ACME");
    postulacion = new Postulacion();
    postulacion.setId(3L);
    postulacion.setIdOferta("LOCAL:4");
    postulacion.setUsuario(candidato);
    Vacante vacante = new Vacante();
    vacante.setId(4L);
    vacante.setEmpleadorId(9L);
    when(repositorioVacante.obtenerPorId(4L)).thenReturn(vacante);
    when(repositorioUsuario.buscarPorId(9L)).thenReturn(empleador);
  }

  @Test
  public void listadoCuentaSoloMensajesNoLeidosDelUsuario() {
    Mensaje recibido = mensaje(empleador, candidato, false, "Hola");
    Mensaje propio = mensaje(candidato, empleador, true, "Buenas");
    when(repositorioMensaje.listarConversacionesDeUsuario(7L))
      .thenReturn(List.of(recibido, propio));

    List<ResumenConversacion> conversaciones = servicio.listarConversaciones(7L);

    assertThat(conversaciones.size(), equalTo(1));
    assertThat(conversaciones.get(0).getNoLeidos(), equalTo(1L));
    assertThat(conversaciones.get(0).getOtroParticipante(), equalTo("ACME"));
  }

  @Test
  public void marcarComoLeidosActualizaElRepositorio() throws AccesoChatDenegado {
    when(repositorioPostulacion.buscarPorId(3L)).thenReturn(postulacion);

    servicio.marcarComoLeidos(3L, 7L);

    verify(repositorioMensaje).marcarLeidos(3L, 7L);
  }

  @Test
  public void usuarioAjenoNoPuedeEnviar() {
    when(repositorioPostulacion.buscarPorId(3L)).thenReturn(postulacion);

    org.junit.jupiter.api.Assertions.assertThrows(
      AccesoChatDenegado.class,
      () -> servicio.enviar(3L, 99L, "No")
    );
  }

  private Mensaje mensaje(Usuario emisor, Usuario receptor, boolean leido, String texto) {
    Mensaje mensaje = new Mensaje();
    mensaje.setId((long) texto.hashCode());
    mensaje.setPostulacion(postulacion);
    mensaje.setEmisor(emisor);
    mensaje.setReceptor(receptor);
    mensaje.setLeido(leido);
    mensaje.setTexto(texto);
    mensaje.setFecha(LocalDateTime.now());
    return mensaje;
  }

  private Usuario usuario(Long id, String nombre) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setNombre(nombre);
    return usuario;
  }
}
