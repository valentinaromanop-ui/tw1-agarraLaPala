package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteSkill;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
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

  @Test
  @Transactional
  @Rollback
  public void deberiaGuardarVacanteConSkillsYBuscarlaPorEmpleador() {
    Vacante vacante = new Vacante();
    vacante.setTitulo("Desarrollador Java");
    vacante.setEmpresa("Empresa de prueba");
    vacante.setSalario(new BigDecimal("1250000.00"));
    vacante.setHorario("Lunes a viernes");
    vacante.setEmpleadorId(42L);
    vacante.setFechaPublicacion(java.time.LocalDateTime.now());
    Skill skill = new Skill();
    skill.setNombre("Java");
    VacanteSkill relacion = new VacanteSkill();
    relacion.setVacante(vacante);
    relacion.setSkill(skill);
    vacante.getVacanteSkills().add(relacion);

    repositorioVacante.guardar(vacante);

    List<Vacante> resultado = repositorioVacante.buscarPorEmpleador(42L);

    assertThat(resultado.size(), equalTo(1));
    assertThat(resultado.get(0).getTitulo(), equalTo("Desarrollador Java"));
    assertThat(resultado.get(0).getSalario(), equalTo(new BigDecimal("1250000.00")));
    assertThat(resultado.get(0).getHorario(), equalTo("Lunes a viernes"));
    assertThat(resultado.get(0).getVacanteSkills().size(), equalTo(1));
    assertThat(
      resultado.get(0).getVacanteSkills().iterator().next().getSkill().getNombre(),
      equalTo("Java")
    );
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaListarSoloLasVacantesDelEmpleadorConsultado() {
    Vacante propia = dadoQueExisteUnaVacante("Oferta propia", true);
    propia.setEmpleadorId(10L);
    Vacante ajena = dadoQueExisteUnaVacante("Oferta ajena", true);
    ajena.setEmpleadorId(20L);

    List<Vacante> resultado = repositorioVacante.buscarPorEmpleador(10L);

    assertThat(resultado, equalTo(List.of(propia)));
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaListarSoloLasOfertasActivasPublicadasPorEmpleadores() {
    Vacante ofertaPublicada = dadoQueExisteUnaVacante("Oferta publicada", true);
    ofertaPublicada.setEmpleadorId(10L);
    Vacante inactiva = dadoQueExisteUnaVacante("Oferta cerrada", false);
    inactiva.setEmpleadorId(10L);
    dadoQueExisteUnaVacante("Oferta externa", true);

    List<Vacante> resultado = repositorioVacante.buscarOfertasPublicadasPorEmpleadores();

    assertThat(resultado, equalTo(List.of(ofertaPublicada)));
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
