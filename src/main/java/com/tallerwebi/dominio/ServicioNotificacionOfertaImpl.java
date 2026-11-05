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
  public List<NotificacionOferta> listarNoLeidas(Long usuarioId) {
    return repositorio.buscarNoLeidas(usuarioId);
  }

  @Override
  public String marcarLeidaYObtenerEnlace(Long notificacionId, Long usuarioId) {
    NotificacionOferta notificacion = repositorio.buscarPorIdYUsuario(notificacionId, usuarioId);
    if (notificacion == null) {
      return null;
    }
    notificacion.setLeida(true);
    return notificacion.getEnlace();
  }
}
