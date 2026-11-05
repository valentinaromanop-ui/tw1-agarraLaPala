package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.EstadoEntrevista;
import com.tallerwebi.dominio.RepositorioEntrevista;
import java.time.Instant;
import java.util.List;
import org.hibernate.LockMode;
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

  @Override
  public Entrevista buscarPorPostulacionParaActualizar(Long postulacionId) {
    Entrevista entrevista = buscarPorPostulacionPorId(postulacionId);
    if (entrevista != null) {
      sessionFactory.getCurrentSession().refresh(entrevista, LockMode.PESSIMISTIC_WRITE);
    }
    return entrevista;
  }

  @Override
  public List<Entrevista> buscarAgendadas(Long empleadorId, Instant desde, Instant hasta) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Entrevista where empleadorId = :empleadorId and estado = :estado " +
        "and inicio < :hasta and fin > :desde",
        Entrevista.class
      )
      .setParameter("empleadorId", empleadorId)
      .setParameter("estado", EstadoEntrevista.AGENDADA)
      .setParameter("desde", desde)
      .setParameter("hasta", hasta)
      .getResultList();
  }
}
