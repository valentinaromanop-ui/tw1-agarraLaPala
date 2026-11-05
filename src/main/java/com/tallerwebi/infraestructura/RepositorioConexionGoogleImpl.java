package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.ConexionGoogleCalendar;
import com.tallerwebi.dominio.RepositorioConexionGoogle;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioConexionGoogle")
public class RepositorioConexionGoogleImpl implements RepositorioConexionGoogle {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioConexionGoogleImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public ConexionGoogleCalendar buscar(Long empleadorId) {
    return sessionFactory.getCurrentSession().get(ConexionGoogleCalendar.class, empleadorId);
  }

  @Override
  public void guardar(ConexionGoogleCalendar conexion) {
    sessionFactory.getCurrentSession().persist(conexion);
  }
}
