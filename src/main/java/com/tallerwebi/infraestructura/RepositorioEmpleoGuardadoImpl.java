package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.EmpleoGuardado;
import com.tallerwebi.dominio.RepositorioEmpleoGuardado;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEmpleoGuardadoImpl implements RepositorioEmpleoGuardado {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioEmpleoGuardadoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public EmpleoGuardado buscarPorUsuarioYOferta(Long usuarioId, String ofertaId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from EmpleoGuardado where usuario.id = :usuarioId and idOferta = :ofertaId",
        EmpleoGuardado.class
      )
      .setParameter("usuarioId", usuarioId)
      .setParameter("ofertaId", ofertaId)
      .uniqueResult();
  }

  @Override
  public List<EmpleoGuardado> buscarPorUsuarioId(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from EmpleoGuardado where usuario.id = :usuarioId order by fechaGuardado desc",
        EmpleoGuardado.class
      )
      .setParameter("usuarioId", usuarioId)
      .getResultList();
  }

  @Override
  public EmpleoGuardado guardar(EmpleoGuardado empleoGuardado) {
    sessionFactory.getCurrentSession().persist(empleoGuardado);
    return empleoGuardado;
  }

  @Override
  public void eliminar(EmpleoGuardado empleoGuardado) {
    sessionFactory.getCurrentSession().remove(empleoGuardado);
  }
}
