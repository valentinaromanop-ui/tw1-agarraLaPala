package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.tallerwebi.dominio.SuscriptorNewsletter;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
public class RepositorioNewsletterTest {

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  @Transactional
  @Rollback
  public void guardaYRecuperaSuscriptorPorCorreo() {
    RepositorioNewsletterImpl repositorio = new RepositorioNewsletterImpl(sessionFactory);
    SuscriptorNewsletter suscriptor = new SuscriptorNewsletter();
    suscriptor.setEmail("newsletter@correo.com");
    suscriptor.setFechaAlta(LocalDateTime.of(2026, 10, 31, 12, 0));

    repositorio.guardar(suscriptor);
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    SuscriptorNewsletter guardado = repositorio.buscarPorEmail("newsletter@correo.com");

    assertThat(guardado.getId(), notNullValue());
    assertThat(guardado.getEmail(), equalTo(suscriptor.getEmail()));
    assertThat(guardado.getFechaAlta(), equalTo(suscriptor.getFechaAlta()));
  }
}
