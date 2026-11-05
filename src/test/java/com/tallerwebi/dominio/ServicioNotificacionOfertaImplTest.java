package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioNotificacionOfertaImplTest {

  private RepositorioNotificacionOferta repositorio;
  private ServicioNotificacionOfertaImpl servicio;
  private Usuario usuario;

  @BeforeEach
  void iniciar() {
    repositorio = mock(RepositorioNotificacionOferta.class);
    servicio = new ServicioNotificacionOfertaImpl(repositorio);
    usuario = new Usuario();
    usuario.setId(42L);
  }

  @Test
  void notificarPersisteUsuarioMensajeYEnlace() {
    servicio.notificar(usuario, "Oferta relacionada", "/vacantes/10");

    org.mockito.ArgumentCaptor<NotificacionOferta> captor = org.mockito.ArgumentCaptor.forClass(
      NotificacionOferta.class
    );
    verify(repositorio).guardar(captor.capture());
    assertSame(usuario, captor.getValue().getUsuario());
    assertEquals("Oferta relacionada", captor.getValue().getMensaje());
    assertEquals("/vacantes/10", captor.getValue().getEnlace());
    assertFalse(captor.getValue().isLeida());
  }

  @Test
  void listarNoLeidasDevuelveLoQueEntregaElRepositorio() {
    List<NotificacionOferta> notificaciones = List.of(new NotificacionOferta());
    when(repositorio.buscarNoLeidas(42L)).thenReturn(notificaciones);

    assertSame(notificaciones, servicio.listarNoLeidas(42L));
    verify(repositorio).buscarNoLeidas(42L);
  }

  @Test
  void marcarLeidaDevuelveEnlaceYActualizaLaNotificacionPropia() {
    NotificacionOferta notificacion = new NotificacionOferta();
    notificacion.setEnlace("/vacantes/10");
    when(repositorio.buscarPorIdYUsuario(8L, 42L)).thenReturn(notificacion);

    String enlace = servicio.marcarLeidaYObtenerEnlace(8L, 42L);

    assertEquals("/vacantes/10", enlace);
    assertTrue(notificacion.isLeida());
  }

  @Test
  void notificacionAjenaOInexistenteNoSeMarcaYDevuelveNull() {
    when(repositorio.buscarPorIdYUsuario(8L, 42L)).thenReturn(null);

    assertNull(servicio.marcarLeidaYObtenerEnlace(8L, 42L));
    verify(repositorio).buscarPorIdYUsuario(8L, 42L);
  }
}
