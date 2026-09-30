package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.VacanteSkill;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VacanteLocalProviderTest {

  private RepositorioVacante repositorioMock;
  private VacanteLocalProvider provider;

  @BeforeEach
  public void init() {
    repositorioMock = mock(RepositorioVacante.class);
    provider = new VacanteLocalProvider(repositorioMock, new VacanteMapper());
  }

  @Test
  public void deberiaMostrarFuenteLocalYSoloSkillsCoincidentes() {
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();
    busqueda.setSkills(Arrays.asList("java", "sql"));
    Vacante vacante = new Vacante();
    vacante.setTitulo("Desarrollador Java");
    Skill java = new Skill();
    java.setNombre("Java");
    Skill docker = new Skill();
    docker.setNombre("docker");
    VacanteSkill vacanteSkillJava = new VacanteSkill();
    vacanteSkillJava.setVacante(vacante);
    vacanteSkillJava.setSkill(java);
    VacanteSkill vacanteSkillDocker = new VacanteSkill();
    vacanteSkillDocker.setVacante(vacante);
    vacanteSkillDocker.setSkill(docker);
    vacante.getVacanteSkills().add(vacanteSkillJava);
    vacante.getVacanteSkills().add(vacanteSkillDocker);
    when(repositorioMock.buscarPorSkills(busqueda.getSkills())).thenReturn(List.of(vacante));

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda);

    assertThat(resultado.getFirst().getFuente(), equalTo("LOCAL"));
    assertThat(resultado.getFirst().getSkills(), equalTo(List.of("java")));
    verify(repositorioMock).buscarPorSkills(busqueda.getSkills());
  }

  @Test
  public void deberiaDevolverListaVaciaCuandoNoHayVacantes() {
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();
    busqueda.setSkills(Arrays.asList("java", "sql"));
    when(repositorioMock.buscarPorSkills(busqueda.getSkills())).thenReturn(Collections.emptyList());

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda);

    assertTrue(resultado.isEmpty());
  }
}
