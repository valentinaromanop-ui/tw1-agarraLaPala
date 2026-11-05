package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.RepositorioEntrevista;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioEntrevista")
public class RepositorioEntrevistaImpl implements RepositorioEntrevista {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioEntrevistaImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(Entrevista entrevista) {
    sessionFactory.getCurrentSession().persist(entrevista);
  }

  @Override
  public Entrevista buscarPorPostulacionPorId(Long postulacionId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Entrevista where postulacion.id = :postulacionId", Entrevista.class)
      .setParameter("postulacionId", postulacionId)
      .uniqueResult();
  }
}
