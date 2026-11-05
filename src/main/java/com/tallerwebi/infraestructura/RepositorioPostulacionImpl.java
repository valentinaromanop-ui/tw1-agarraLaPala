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
        "from Postulacion where usuario.id = :usuarioId and idOferta = :ofertaId",
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

  @Override
  public long contarPorVacante(Long vacanteId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(p.id) from Postulacion p " +
        "where p.idOferta = :ofertaLocal or p.idOferta = :idVacante",
        Long.class
      )
      .setParameter("ofertaLocal", "LOCAL:" + vacanteId)
      .setParameter("idVacante", String.valueOf(vacanteId))
      .getSingleResult();
  }

  @Override
  public List<Postulacion> buscarPorVacante(Long vacanteId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select distinct p from Postulacion p join fetch p.usuario usuario " +
        "left join fetch usuario.habilidades " +
        "where p.idOferta = :ofertaLocal or p.idOferta = :idVacante " +
        "order by p.fechaPostulacion desc",
        Postulacion.class
      )
      .setParameter("ofertaLocal", "LOCAL:" + vacanteId)
      .setParameter("idVacante", String.valueOf(vacanteId))
      .getResultList();
  }

  @Override
  public Postulacion buscarPorIdYVacante(Long postulacionId, Long vacanteId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select distinct p from Postulacion p join fetch p.usuario " +
        "where p.id = :postulacionId and " +
        "(p.idOferta = :ofertaLocal or p.idOferta = :idVacante)",
        Postulacion.class
      )
      .setParameter("postulacionId", postulacionId)
      .setParameter("ofertaLocal", "LOCAL:" + vacanteId)
      .setParameter("idVacante", String.valueOf(vacanteId))
      .uniqueResult();
  }

  @Override
  public Postulacion buscarPorId(Long postulacionId) {
    return sessionFactory.getCurrentSession().get(Postulacion.class, postulacionId);
  }
}
