package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioNotificacionOfertaImpl implements ServicioNotificacionOferta {

  private final RepositorioNotificacionOferta repositorio;

  @Autowired
  public ServicioNotificacionOfertaImpl(RepositorioNotificacionOferta repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public void notificar(Usuario usuario, String mensaje, String enlace) {
    NotificacionOferta notificacion = new NotificacionOferta();
    notificacion.setUsuario(usuario);
    notificacion.setMensaje(mensaje);
    notificacion.setEnlace(enlace);
    repositorio.guardar(notificacion);
  }

  @Override
  public List<NotificacionOferta> listar(Long usuarioId) {
    return repositorio.buscarPorUsuario(usuarioId);
  }

  @Override
  public List<NotificacionOferta> listarNoLeidas(Long usuarioId) {
    return repositorio.buscarNoLeidas(usuarioId);
  }

  @Override
  public long contarNoLeidas(Long usuarioId) {
    return repositorio.contarNoLeidas(usuarioId);
  }

  @Override
  public String marcarLeidaYObtenerEnlace(Long notificacionId, Long usuarioId) {
    NotificacionOferta notificacion = repositorio.buscarPorIdYUsuario(notificacionId, usuarioId);
    if (notificacion == null) {
      return null;
    }
    notificacion.setLeida(true);
    repositorio.guardar(notificacion);
    return notificacion.getEnlace();
  }

  @Override
  public void marcarTodasLeidas(Long usuarioId) {
    repositorio.marcarTodasLeidas(usuarioId);
  }

  @Override
  public void notificarDecision(Postulacion postulacion, EstadoPostulacion nuevoEstado) {
    if (nuevoEstado != EstadoPostulacion.ACEPTADA && nuevoEstado != EstadoPostulacion.RECHAZADA) {
      return;
    }
    String resultado = nuevoEstado == EstadoPostulacion.ACEPTADA ? "aceptada" : "rechazada";
    String oferta = postulacion.getTitulo() == null ? "la oferta" : postulacion.getTitulo();

    // Llamada directa al método notificar de esta misma clase
    notificar(
      postulacion.getUsuario(),
      "Tu postulación a " + oferta + " fue " + resultado,
      "/postulaciones"
    );
  }
}
