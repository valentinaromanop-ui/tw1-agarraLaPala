package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Invitacion;
import com.tallerwebi.dominio.RepositorioInvitacion;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioInvitacionImpl implements RepositorioInvitacion {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioInvitacionImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Invitacion guardar(Invitacion invitacion) {
    sessionFactory.getCurrentSession().persist(invitacion);
    return invitacion;
  }

  @Override
  public Invitacion buscarPorCandidatoYOferta(Long candidatoId, Long ofertaId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Invitacion where candidato.id = :candidatoId and oferta.id = :ofertaId",
        Invitacion.class
      )
      .setParameter("candidatoId", candidatoId)
      .setParameter("ofertaId", ofertaId)
      .uniqueResult();
  }

  @Override
  public List<Invitacion> buscarPorOferta(Long ofertaId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Invitacion where oferta.id = :ofertaId", Invitacion.class)
      .setParameter("ofertaId", ofertaId)
      .getResultList();
  }

  @Override
  public List<Invitacion> buscarPorCandidato(Long candidatoId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Invitacion where candidato.id = :candidatoId order by fecha desc",
        Invitacion.class
      )
      .setParameter("candidatoId", candidatoId)
      .getResultList();
  }

  @Override
  public Invitacion buscarPorIdYCandidato(Long invitacionId, Long candidatoId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Invitacion where id = :invitacionId and candidato.id = :candidatoId",
        Invitacion.class
      )
      .setParameter("invitacionId", invitacionId)
      .setParameter("candidatoId", candidatoId)
      .uniqueResult();
  }
}
