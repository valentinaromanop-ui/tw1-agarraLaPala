package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.RepositorioMensaje;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository("repositorioMensaje")
@Transactional
public class RepositorioMensajeImpl implements RepositorioMensaje {

  private static final String PARAMETRO_USUARIO_ID = "usuarioId";
  private static final String CONDICION_NO_LEIDO = "leido = false";
  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioMensajeImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Mensaje guardarMensaje(Mensaje mensaje) {
    sessionFactory.getCurrentSession().persist(mensaje);
    return mensaje;
  }

  @Override
  public Mensaje guardar(Mensaje mensaje) {
    return guardarMensaje(mensaje);
  }

  @Override
  public List<Mensaje> listarPorPostulacion(Long postulacionId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Mensaje where postulacion.id = :postulacionId order by fecha asc, id asc",
        Mensaje.class
      )
      .setParameter("postulacionId", postulacionId)
      .getResultList();
  }

  @Override
  public List<Mensaje> listarConversacionesDeUsuario(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Mensaje where emisor.id = :id or receptor.id = :id order by fecha desc",
        Mensaje.class
      )
      .setParameter("id", usuarioId)
      .getResultList();
  }

  @Override
  public List<Mensaje> listarNoLeidos(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Mensaje mensaje where mensaje.receptor.id = :usuarioId " +
        "and " +
        CONDICION_NO_LEIDO +
        " order by mensaje.fecha desc",
        Mensaje.class
      )
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .getResultList();
  }

  @Override
  public long contarNoLeidos(Long postulacionId, Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(m.id) from Mensaje m " +
        "where m.postulacion.id = :postulacionId and m.emisor.id <> :" +
        PARAMETRO_USUARIO_ID +
        " and " +
        CONDICION_NO_LEIDO,
        Long.class
      )
      .setParameter("postulacionId", postulacionId)
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .getSingleResult();
  }

  @Override
  public void marcarLeidos(Long postulacionId, Long usuarioId) {
    sessionFactory
      .getCurrentSession()
      .createMutationQuery(
        "update Mensaje m set m.leido = true " +
        "where m.postulacion.id = :postulacionId and m.emisor.id <> :" +
        PARAMETRO_USUARIO_ID +
        " and " +
        CONDICION_NO_LEIDO
      )
      .setParameter("postulacionId", postulacionId)
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .executeUpdate();
  }

  @Override
  public void marcarTodosLeidos(Long usuarioId) {
    sessionFactory
      .getCurrentSession()
      .createMutationQuery(
        "update Mensaje m set m.leido = true " +
        "where m.receptor.id = :" +
        PARAMETRO_USUARIO_ID +
        " and " +
        CONDICION_NO_LEIDO
      )
      .setParameter(PARAMETRO_USUARIO_ID, usuarioId)
      .executeUpdate();
  }
}
