package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.ReporteResena;
import com.tallerwebi.dominio.RepositorioResena;
import com.tallerwebi.dominio.Resena;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioResena")
public class RepositorioResenaImpl implements RepositorioResena {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioResenaImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<Resena> buscarPorEmpleador(Long empleadorId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select r from Resena r join fetch r.candidato " +
        "where r.empleador.id = :empleadorId order by r.fecha desc, r.id desc",
        Resena.class
      )
      .setParameter("empleadorId", empleadorId)
      .getResultList();
  }

  @Override
  public List<Resena> buscarPorEmpleadores(List<Long> empleadorIds) {
    if (empleadorIds == null || empleadorIds.isEmpty()) {
      return List.of();
    }
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Resena r where r.empleador.id in (:empleadorIds)", Resena.class)
      .setParameterList("empleadorIds", empleadorIds)
      .getResultList();
  }

  @Override
  public Resena buscarPorCandidatoYEmpleador(Long candidatoId, Long empleadorId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Resena where candidato.id = :candidatoId and empleador.id = :empleadorId",
        Resena.class
      )
      .setParameter("candidatoId", candidatoId)
      .setParameter("empleadorId", empleadorId)
      .uniqueResult();
  }

  @Override
  public Resena buscarPorId(Long resenaId) {
    return sessionFactory.getCurrentSession().get(Resena.class, resenaId);
  }

  @Override
  public Resena buscarPorIdParaActualizar(Long resenaId) {
    return sessionFactory
      .getCurrentSession()
      .find(Resena.class, resenaId, LockModeType.PESSIMISTIC_WRITE);
  }

  @Override
  public Resena guardar(Resena resena) {
    if (resena.getId() == null) {
      sessionFactory.getCurrentSession().persist(resena);
      return resena;
    }
    return sessionFactory.getCurrentSession().merge(resena);
  }

  @Override
  public void eliminar(Resena resena) {
    sessionFactory.getCurrentSession().remove(resena);
  }

  @Override
  public void guardarReporte(ReporteResena reporte) {
    sessionFactory.getCurrentSession().persist(reporte);
  }
}
