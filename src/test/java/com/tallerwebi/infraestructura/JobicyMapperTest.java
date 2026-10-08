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
    String json = "{\"jobLevel\":\"Junior\",\"jobType\":[\"Full-Time\"]," +
      "\"salaryMin\":1500,\"salaryCurrency\":\"USD\",\"salaryPeriod\":\"monthly\"}";

    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree(json));

    assertEquals("remote", vacante.getCondiciones().getModalidad());
    assertEquals("junior", vacante.getCondiciones().getSeniority());
    assertEquals("full-time", vacante.getCondiciones().getJornada());
    assertEquals(new BigDecimal("1500"), vacante.getCondiciones().getSueldoMinimo());
    assertEquals("USD", vacante.getCondiciones().getMoneda());
  }

  @Test
  public void deberiaLeerNivelIntermedioYJornadaParcial() throws Exception {
    String json = "{\"jobLevel\":\"Mid-Level\",\"jobType\":[\"Part-Time\"],\"salaryPeriod\":\"yearly\"}";

    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree(json));

    assertEquals("mid-level", vacante.getCondiciones().getSeniority());
    assertEquals("part-time", vacante.getCondiciones().getJornada());
  }

  @Test
  public void deberiaDejarSinInformarLosCamposAusentes() throws Exception {
    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree("{}"));

    assertNull(vacante.getCondiciones().getSeniority());
    assertNull(vacante.getCondiciones().getJornada());
    assertNull(vacante.getCondiciones().getSueldoMinimo());
    assertNull(vacante.getCondiciones().getMoneda());
  }

  @Test
  public void noDeberiaTomarUnSueldoAnualComoMensual() throws Exception {
    String json = "{\"salaryMin\":12000,\"salaryPeriod\":\"yearly\"}";

    VacanteDTO vacante = new JobicyMapper().toDTO(new ObjectMapper().readTree(json));

    assertNull(vacante.getCondiciones().getSueldoMinimo());
  }
}
