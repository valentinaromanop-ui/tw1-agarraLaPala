package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Pago;
import com.tallerwebi.dominio.RepositorioPago;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioPagoImpl implements RepositorioPago {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioPagoImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Pago guardar(Pago pago) {
    sessionFactory.getCurrentSession().persist(pago);
    return pago;
  }

  @Override
  public List<Pago> buscarPorUsuarioId(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Pago where usuario.id = :usuarioId order by fecha desc", Pago.class)
      .setParameter("usuarioId", usuarioId)
      .getResultList();
  }
}
