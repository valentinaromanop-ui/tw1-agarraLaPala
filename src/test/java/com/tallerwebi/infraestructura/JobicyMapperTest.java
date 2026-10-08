package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.VacanteDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class JobicyMapperTest {

  @Test
  public void deberiaConservarValoresEnInglesEnMinusculas() throws Exception {
    String json =
      "{\"jobLevel\":\"Junior\",\"jobType\":[\"Full-Time\"]," +
      "\"salaryMin\":1500,\"salaryCurrency\":\"USD\",\"salaryPeriod\":\"monthly\"}";

    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree(json));

    assertEquals("remote", vacante.getModalidad());
    assertEquals("junior", vacante.getSeniority());
    assertEquals("full-time", vacante.getJornada());
    assertEquals(new BigDecimal("1500"), vacante.getSalario());
    assertEquals("USD", vacante.getMoneda());
  }

  @Test
  public void deberiaLeerNivelIntermedioYJornadaParcial() throws Exception {
    String json =
      "{\"jobLevel\":\"Mid-Level\",\"jobType\":[\"Part-Time\"],\"salaryPeriod\":\"yearly\"}";

    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree(json));

    assertEquals("mid-level", vacante.getSeniority());
    assertEquals("part-time", vacante.getJornada());
  }

  @Test
  public void deberiaDejarSinInformarLosCamposAusentes() throws Exception {
    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree("{}"));

    assertNull(vacante.getSeniority());
    assertNull(vacante.getJornada());
    assertNull(vacante.getSalario());
    assertNull(vacante.getMoneda());
  }

  @Test
  public void noDeberiaTomarUnSueldoAnualComoMensual() throws Exception {
    String json = "{\"salaryMin\":12000,\"salaryPeriod\":\"yearly\"}";

    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree(json));

    assertNull(vacante.getSalario());
  }
}
