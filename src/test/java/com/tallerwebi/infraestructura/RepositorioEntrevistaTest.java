package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.EstadoEntrevista;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.RepositorioEntrevista;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HibernateInfraestructuraTestConfig.class })
public class RepositorioEntrevistaTest {

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioEntrevista repositorioEntrevista;

  @BeforeEach
  public void init() {
    repositorioEntrevista = new RepositorioEntrevistaImpl(sessionFactory);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaGuardarUnaInvitacionConSuPostulacion() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacion();
    LocalDateTime fecha = LocalDateTime.of(2026, 11, 4, 10, 0);
    Entrevista entrevista = new Entrevista(postulacion, fecha);

    // ejecucion
    repositorioEntrevista.guardar(entrevista);
    sincronizarLaBase();

    // validacion
    Entrevista guardada = sessionFactory
      .getCurrentSession()
      .createQuery("from Entrevista where postulacion.id = :postulacionId", Entrevista.class)
      .setParameter("postulacionId", postulacion.getId())
      .getSingleResult();
    assertThat(guardada.getPostulacion().getId(), equalTo(postulacion.getId()));
    assertThat(guardada.getFechaInvitacion(), equalTo(fecha));
    assertThat(guardada.getEstado(), equalTo(EstadoEntrevista.INVITADA));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaBuscarLaInvitacionPorPostulacion() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacion();
    sessionFactory.getCurrentSession().persist(new Entrevista(postulacion, LocalDateTime.now()));
    sincronizarLaBase();

    // ejecucion
    Entrevista obtenida = repositorioEntrevista.buscarPorPostulacionPorId(postulacion.getId());

    // validacion
    assertThat(obtenida.getPostulacion().getId(), equalTo(postulacion.getId()));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaDevolverNullCuandoNoExisteInvitacion() {
    // ejecucion
    Entrevista obtenida = repositorioEntrevista.buscarPorPostulacionPorId(999L);

    // validacion
    assertThat(obtenida, nullValue());
  }

  @Test
  @Transactional
  @Rollback
  public void noDeberiaGuardarDosEntrevistasParaLaMismaPostulacion() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacion();
    repositorioEntrevista.guardar(new Entrevista(postulacion, LocalDateTime.now()));
    sessionFactory.getCurrentSession().flush();

    // ejecucion y validacion
    assertThrows(
      PersistenceException.class,
      () -> {
        repositorioEntrevista.guardar(new Entrevista(postulacion, LocalDateTime.now()));
        sessionFactory.getCurrentSession().flush();
      }
    );
  }

  private Postulacion dadoQueExisteUnaPostulacion() {
    Usuario candidato = new Usuario();
    candidato.setEmail("entrevista@test.com");
    candidato.setPassword("test");
    candidato.setRol("CANDIDATO");
    sessionFactory.getCurrentSession().persist(candidato);
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(candidato);
    postulacion.setIdOferta("LOCAL:20");
    postulacion.setFechaPostulacion(LocalDateTime.now());
    sessionFactory.getCurrentSession().persist(postulacion);
    return postulacion;
  }

  private void sincronizarLaBase() {
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
  }
}
