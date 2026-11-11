package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioNotificacionOferta {
  NotificacionOferta guardar(NotificacionOferta notificacion);
  List<NotificacionOferta> buscarPorUsuario(Long usuarioId);
  List<NotificacionOferta> buscarNoLeidas(Long usuarioId);
  long contarNoLeidas(Long usuarioId);
  NotificacionOferta buscarPorIdYUsuario(Long notificacionId, Long usuarioId);
}
