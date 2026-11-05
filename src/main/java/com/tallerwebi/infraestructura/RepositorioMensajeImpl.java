package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.RepositorioMensaje;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioMensaje")
public class RepositorioMensajeImpl implements RepositorioMensaje {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioMensajeImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<Mensaje> listarPorPostulacion(Long postulacionId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Mensaje where postulacion.id = :postulacionId order by fechaEnvio asc, id asc",
        Mensaje.class
      )
      .setParameter("postulacionId", postulacionId)
      .getResultList();
  }

  @Override
  public long contarNoLeidos(Long postulacionId, Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(m.id) from Mensaje m " +
        "where m.postulacion.id = :postulacionId and m.remitente.id <> :usuarioId and m.leido = false",
        Long.class
      )
      .setParameter("postulacionId", postulacionId)
      .setParameter("usuarioId", usuarioId)
      .getSingleResult();
  }

  @Override
  public void marcarLeidos(Long postulacionId, Long usuarioId) {
    sessionFactory
      .getCurrentSession()
      .createMutationQuery(
        "update Mensaje m set m.leido = true " +
        "where m.postulacion.id = :postulacionId and m.remitente.id <> :usuarioId and m.leido = false"
      )
      .setParameter("postulacionId", postulacionId)
      .setParameter("usuarioId", usuarioId)
      .executeUpdate();
  }

  @Override
  public Mensaje guardar(Mensaje mensaje) {
    sessionFactory.getCurrentSession().persist(mensaje);
    return mensaje;
  }
}
