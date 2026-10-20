package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.RepositorioCurriculumGenerado;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCurriculumGeneradoImpl implements RepositorioCurriculumGenerado {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioCurriculumGeneradoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public CurriculumGenerado buscarPorUsuarioId(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from CurriculumGenerado where usuario.id = :usuarioId",
        CurriculumGenerado.class
      )
      .setParameter("usuarioId", usuarioId)
      .uniqueResult();
  }

  @Override
  public CurriculumGenerado buscarPorUsuarioIdParaActualizar(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from CurriculumGenerado where usuario.id = :usuarioId",
        CurriculumGenerado.class
      )
      .setParameter("usuarioId", usuarioId)
      .setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .uniqueResult();
  }

  @Override
  public CurriculumGenerado guardar(CurriculumGenerado curriculum) {
    if (curriculum.getId() == null) {
      sessionFactory.getCurrentSession().persist(curriculum);
      return curriculum;
    }
    return sessionFactory.getCurrentSession().merge(curriculum);
  }

  @Override
  public List<Long> buscarUsuarioIdsConCvOriginal(List<Long> usuarioIds) {
    if (usuarioIds.isEmpty()) {
      return List.of();
    }
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select curriculum.usuario.id from CurriculumGenerado curriculum " +
        "where curriculum.usuario.id in :usuarioIds " +
        "and curriculum.archivoOriginal is not null " +
        "and curriculum.nombreArchivoOriginal is not null",
        Long.class
      )
      .setParameter("usuarioIds", usuarioIds)
      .getResultList();
  }
}
