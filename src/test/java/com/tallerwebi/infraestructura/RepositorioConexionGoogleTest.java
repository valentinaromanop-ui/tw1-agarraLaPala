package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.tallerwebi.dominio.ConexionGoogleCalendar;
import com.tallerwebi.dominio.RepositorioConexionGoogle;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
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
public class RepositorioConexionGoogleTest {

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioConexionGoogle repositorioConexionGoogle;

  @BeforeEach
  public void init() {
    repositorioConexionGoogle = new RepositorioConexionGoogleImpl(sessionFactory);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaGuardarUnaConexionDelEmpleador() {
    // preparacion
    Usuario empleador = dadoQueExisteUnEmpleador();
    ConexionGoogleCalendar conexion = new ConexionGoogleCalendar(empleador, "cifrado");

    // ejecucion
    cuandoGuardoLaConexion(conexion);

    // validacion
    entoncesSeGuardoLaConexion(empleador.getId(), "cifrado");
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaEncontrarLaConexionPorEmpleador() {
    // preparacion
    Usuario empleador = dadoQueExisteUnEmpleador();
    dadoQueExisteUnaConexion(empleador, "cifrado");

    // ejecucion
    ConexionGoogleCalendar obtenida = cuandoBuscoLaConexion(empleador.getId());

    // validacion
    assertThat(obtenida.getRefreshTokenCifrado(), equalTo("cifrado"));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaActualizarElTokenDeUnaConexionExistente() {
    // preparacion
    Usuario empleador = dadoQueExisteUnEmpleador();
    dadoQueExisteUnaConexion(empleador, "anterior");
    ConexionGoogleCalendar conexion = cuandoBuscoLaConexion(empleador.getId());

    // ejecucion
    conexion.setRefreshTokenCifrado("nuevo");
    sincronizarLaBase();

    // validacion
    entoncesSeGuardoLaConexion(empleador.getId(), "nuevo");
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaDevolverNullSiNoExisteConexion() {
    // ejecucion
    ConexionGoogleCalendar obtenida = cuandoBuscoLaConexion(999L);

    // validacion
    assertThat(obtenida, nullValue());
  }

  private Usuario dadoQueExisteUnEmpleador() {
    Usuario empleador = new Usuario();
    empleador.setEmail("calendar@test.com");
    empleador.setPassword("test");
    empleador.setRol("EMPLEADOR");
    sessionFactory.getCurrentSession().persist(empleador);
    return empleador;
  }

  private void dadoQueExisteUnaConexion(Usuario empleador, String tokenCifrado) {
    sessionFactory.getCurrentSession().persist(new ConexionGoogleCalendar(empleador, tokenCifrado));
    sincronizarLaBase();
  }

  private void cuandoGuardoLaConexion(ConexionGoogleCalendar conexion) {
    repositorioConexionGoogle.guardar(conexion);
    sincronizarLaBase();
  }

  private ConexionGoogleCalendar cuandoBuscoLaConexion(Long empleadorId) {
    return repositorioConexionGoogle.buscar(empleadorId);
  }

  private void entoncesSeGuardoLaConexion(Long empleadorId, String tokenEsperado) {
    ConexionGoogleCalendar obtenida = sessionFactory
      .getCurrentSession()
      .createQuery(
        "FROM ConexionGoogleCalendar WHERE empleadorId = :empleadorId",
        ConexionGoogleCalendar.class
      )
      .setParameter("empleadorId", empleadorId)
      .getSingleResult();
    assertThat(obtenida.getRefreshTokenCifrado(), equalTo(tokenEsperado));
  }

  private void sincronizarLaBase() {
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
  }
}
