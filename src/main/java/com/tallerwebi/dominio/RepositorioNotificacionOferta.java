package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioNotificacionOferta {
  NotificacionOferta guardar(NotificacionOferta notificacion);
  List<NotificacionOferta> buscarNoLeidas(Long usuarioId);
  NotificacionOferta buscarPorIdYUsuario(Long notificacionId, Long usuarioId);
}
