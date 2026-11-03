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
}
