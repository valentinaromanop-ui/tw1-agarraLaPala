package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioNewsletter;
import com.tallerwebi.dominio.SuscriptorNewsletter;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioNewsletterImpl implements RepositorioNewsletter {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioNewsletterImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public SuscriptorNewsletter buscarPorEmail(String email) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from SuscriptorNewsletter where email = :email", SuscriptorNewsletter.class)
      .setParameter("email", email)
      .uniqueResult();
  }

  @Override
  public void guardar(SuscriptorNewsletter suscriptor) {
    sessionFactory.getCurrentSession().persist(suscriptor);
  }
}
