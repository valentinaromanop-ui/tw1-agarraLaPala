package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.RepositorioPostulacion;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPostulacion")
public class RepositorioPostulacionImpl implements RepositorioPostulacion {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioPostulacionImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Postulacion guardar(Postulacion postulacion) {
    sessionFactory.getCurrentSession().persist(postulacion);
    return postulacion;
  }

  @Override
  public Postulacion buscarPorUsuarioYOferta(Long usuarioId, String ofertaId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Postulacion where usuario.id = :usuarioId and ofertaId = :ofertaId",
        Postulacion.class
      )
      .setParameter("usuarioId", usuarioId)
      .setParameter("ofertaId", ofertaId)
      .uniqueResult();
  }

  @Override
  public List<Postulacion> buscarPorUsuarioId(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Postulacion where usuario.id = :usuarioId order by fechaPostulacion desc",
        Postulacion.class
      )
      .setParameter("usuarioId", usuarioId)
      .getResultList();
  }
}
