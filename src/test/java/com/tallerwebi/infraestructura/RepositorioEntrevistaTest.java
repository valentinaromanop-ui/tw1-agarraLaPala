package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.EstadoEntrevista;
import com.tallerwebi.dominio.EventoGoogleCalendar;
import com.tallerwebi.dominio.IntervaloHorario;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.RepositorioEntrevista;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

  @Test
  @Transactional
  @Rollback
  public void deberiaGuardarLaReservaYElEnlaceDeMeet() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacion();
    Entrevista entrevista = new Entrevista(postulacion, LocalDateTime.now());
    entrevista.setEmpleadorId(7L);
    repositorioEntrevista.guardar(entrevista);
    sincronizarLaBase();

    // ejecucion
    Entrevista paraActualizar = repositorioEntrevista.buscarPorPostulacionParaActualizar(
      postulacion.getId()
    );
    paraActualizar.agendar(
      new IntervaloHorario(
        Instant.parse("2026-11-05T12:00:00Z"),
        Instant.parse("2026-11-05T12:30:00Z")
      ),
      new EventoGoogleCalendar("evento-google", "https://meet.google.com/abc-defg-hij")
    );
    sincronizarLaBase();

    // validacion
    Entrevista guardada = repositorioEntrevista.buscarPorPostulacionPorId(postulacion.getId());
    assertThat(guardada.getEstado(), equalTo(EstadoEntrevista.AGENDADA));
    assertThat(guardada.getInicio(), equalTo(Instant.parse("2026-11-05T12:00:00Z")));
    assertThat(guardada.getFin(), equalTo(Instant.parse("2026-11-05T12:30:00Z")));
    assertThat(guardada.getGoogleEventId(), equalTo("evento-google"));
    assertThat(guardada.getUrlMeet(), equalTo("https://meet.google.com/abc-defg-hij"));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaFiltrarReservasPorEmpleadorYPorSuperposicion() {
    // preparacion
    Entrevista esperada = dadoQueExisteUnaReserva(7L, "2026-11-05T12:00:00Z");
    dadoQueExisteUnaReserva(7L, "2026-11-05T11:30:00Z");
    dadoQueExisteUnaReserva(7L, "2026-11-05T12:30:00Z");
    dadoQueExisteUnaReserva(8L, "2026-11-05T12:00:00Z");
    sincronizarLaBase();

    // ejecucion
    List<Entrevista> resultado = repositorioEntrevista.buscarAgendadas(
      7L,
      Instant.parse("2026-11-05T12:00:00Z"),
      Instant.parse("2026-11-05T12:30:00Z")
    );

    // validacion
    assertThat(resultado.size(), equalTo(1));
    assertThat(resultado.getFirst().getId(), equalTo(esperada.getId()));
  }

  private Entrevista dadoQueExisteUnaReserva(Long empleadorId, String inicio) {
    Postulacion postulacion = dadoQueExisteUnaPostulacion();
    Entrevista entrevista = new Entrevista(postulacion, LocalDateTime.now());
    entrevista.setEmpleadorId(empleadorId);
    Instant comienzo = Instant.parse(inicio);
    entrevista.agendar(
      new IntervaloHorario(comienzo, comienzo.plus(Duration.ofMinutes(30))),
      new EventoGoogleCalendar("evento-" + postulacion.getId(), null)
    );
    repositorioEntrevista.guardar(entrevista);
    return entrevista;
  }

  private Postulacion dadoQueExisteUnaPostulacion() {
    Usuario candidato = new Usuario();
    candidato.setEmail(UUID.randomUUID() + "@test.com");
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
