package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioSuscripcion;
import com.tallerwebi.dominio.Suscripcion;
import java.time.LocalDateTime;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioSuscripcionImpl implements RepositorioSuscripcion {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioSuscripcionImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Suscripcion buscarPorUsuarioId(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Suscripcion where usuario.id = :usuarioId", Suscripcion.class)
      .setParameter("usuarioId", usuarioId)
      .uniqueResult();
  }

  @Override
  public Suscripcion guardar(Suscripcion suscripcion) {
    if (suscripcion.getId() == null) {
      sessionFactory.getCurrentSession().persist(suscripcion);
    } else {
      sessionFactory.getCurrentSession().merge(suscripcion);
    }
    return suscripcion;
  }

  @Override
  public boolean estaActiva(Long usuarioId, LocalDateTime ahora) {
    return (
      sessionFactory
        .getCurrentSession()
        .createQuery(
          "select count(s) from Suscripcion s " +
          "where s.usuario.id = :usuarioId and s.fechaFin > :ahora",
          Long.class
        )
        .setParameter("usuarioId", usuarioId)
        .setParameter("ahora", ahora)
        .uniqueResult() >
      0
    );
  }
}
