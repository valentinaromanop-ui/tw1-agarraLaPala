package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteSkill;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
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
public class RepositorioVacanteTest {

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioVacante repositorioVacante;

  @BeforeEach
  public void init() {
    repositorioVacante = new RepositorioVacanteImpl(sessionFactory);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaEncontrarVacantesSinRepetirPorCadaSkill() {
    Vacante vacante = dadoQueExisteUnaVacante("Desarrollador Java", true);
    dadoQueLaVacanteTieneUnaSkill(vacante, "java");
    dadoQueLaVacanteTieneUnaSkill(vacante, "sql");

    List<Vacante> resultado = cuandoBuscoVacantesPorSkills(Arrays.asList("java", "sql"));

    assertThat(resultado, equalTo(List.of(vacante)));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaExcluirLasVacantesInactivas() {
    Vacante activa = dadoQueExisteUnaVacante("Desarrollador Java", true);
    Vacante inactiva = dadoQueExisteUnaVacante("Busqueda cerrada", false);
    dadoQueLaVacanteTieneUnaSkill(activa, "java");
    dadoQueLaVacanteTieneUnaSkill(inactiva, "java");

    List<Vacante> resultado = cuandoBuscoVacantesPorSkills(List.of("java"));

    assertThat(resultado, equalTo(List.of(activa)));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaDevolverListaVaciaSiNoCoincidenLasSkills() {
    Vacante vacante = dadoQueExisteUnaVacante("Desarrollador Java", true);
    dadoQueLaVacanteTieneUnaSkill(vacante, "java");

    List<Vacante> resultado = cuandoBuscoVacantesPorSkills(List.of("php"));

    assertThat(resultado.isEmpty(), equalTo(true));
  }

  private Vacante dadoQueExisteUnaVacante(String titulo, Boolean activa) {
    Vacante vacante = new Vacante();
    vacante.setTitulo(titulo);
    vacante.setActiva(activa);
    sessionFactory.getCurrentSession().persist(vacante);
    return vacante;
  }

  private void dadoQueLaVacanteTieneUnaSkill(Vacante vacante, String nombre) {
    Skill skill = new Skill();
    skill.setNombre(nombre);
    sessionFactory.getCurrentSession().persist(skill);

    VacanteSkill vacanteSkill = new VacanteSkill();
    vacanteSkill.setVacante(vacante);
    vacanteSkill.setSkill(skill);
    sessionFactory.getCurrentSession().persist(vacanteSkill);
  }

  private List<Vacante> cuandoBuscoVacantesPorSkills(List<String> skills) {
    return repositorioVacante.buscarPorSkills(skills);
  }
}
