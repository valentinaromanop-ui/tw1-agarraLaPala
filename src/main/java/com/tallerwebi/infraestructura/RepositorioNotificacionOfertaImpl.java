package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.NotificacionOferta;
import com.tallerwebi.dominio.RepositorioNotificacionOferta;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioNotificacionOfertaImpl implements RepositorioNotificacionOferta {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioNotificacionOfertaImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public NotificacionOferta guardar(NotificacionOferta notificacion) {
    sessionFactory.getCurrentSession().persist(notificacion);
    return notificacion;
  }

  @Override
  public List<NotificacionOferta> buscarNoLeidas(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from NotificacionOferta where usuario.id = :usuarioId and leida = false order by fecha desc",
        NotificacionOferta.class
      )
      .setParameter("usuarioId", usuarioId)
      .getResultList();
  }

  @Override
  public NotificacionOferta buscarPorIdYUsuario(Long notificacionId, Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from NotificacionOferta where id = :id and usuario.id = :usuarioId",
        NotificacionOferta.class
      )
      .setParameter("id", notificacionId)
      .setParameter("usuarioId", usuarioId)
      .uniqueResult();
  }
}
