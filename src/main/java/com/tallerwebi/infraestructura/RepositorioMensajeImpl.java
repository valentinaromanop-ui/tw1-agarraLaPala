package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.RepositorioMensaje;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class RepositorioMensajeImpl implements RepositorioMensaje {

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
  public List<Mensaje> listarPorPostulacion(Long id) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("FROM Mensaje m WHERE m.postulacion.id = :id", Mensaje.class)
      .setParameter("id", id)
      .getResultList();
  }

  @Override
  public List<Mensaje> listarNoLeidos(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "FROM Mensaje mensaje where mensaje.receptor.id = :id AND mensaje.leido = false ORDER BY mensaje.fecha DESC",
        Mensaje.class
      )
      .setParameter("id", usuarioId)
      .getResultList();
  }

  @Override
  public void marcarLeidos(Long postulacionId, Long usuarioId) {
    sessionFactory
      .getCurrentSession()
      .createMutationQuery(
        "UPDATE Mensaje mensaje SET mensaje.leido = true " +
        "WHERE mensaje.postulacion.id = :postulacionId AND mensaje.receptor.id = :usuarioId AND mensaje.leido = false"
      )
      .setParameter("postulacionId", postulacionId)
      .setParameter("usuarioId", usuarioId)
      .executeUpdate();
  }
}
