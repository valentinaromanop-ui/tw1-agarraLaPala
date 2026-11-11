package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.NotificacionOferta;
import com.tallerwebi.dominio.RepositorioNotificacionOferta;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioNotificacionOfertaImpl implements RepositorioNotificacionOferta {

  private static final String PARAMETRO_USUARIO_ID = "usuarioId";
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
  public List<NotificacionOferta> buscarPorUsuario(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from NotificacionOferta where usuario.id = :" +
        PARAMETRO_USUARIO_ID +
        " order by fecha desc",
        NotificacionOferta.class
      )
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .getResultList();
  }

  @Override
  public List<NotificacionOferta> buscarNoLeidas(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from NotificacionOferta where usuario.id = :" +
        PARAMETRO_USUARIO_ID +
        " and leida = false order by fecha desc",
        NotificacionOferta.class
      )
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .getResultList();
  }

  @Override
  public long contarNoLeidas(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(n.id) from NotificacionOferta n where n.usuario.id = :" +
        PARAMETRO_USUARIO_ID +
        " and n.leida = false",
        Long.class
      )
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .getSingleResult();
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
