package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

public class ControladorBusquedaVacantesTest {

  private MockMvc mockMvc;

  @BeforeEach
  public void iniciar() {
    ControladorBusquedaVacantes controlador = new ControladorBusquedaVacantes(
      new CatalogoVacantesPrueba()
    );
    mockMvc = MockMvcBuilders.standaloneSetup(controlador).build();
  }

  @Test
  public void mostrarBusquedaDevuelveLaVistaExistente() {
    ControladorBusquedaVacantes controlador = new ControladorBusquedaVacantes(
      new CatalogoVacantesPrueba()
    );

    ModelAndView resultado = controlador.mostrarBusqueda("backend");

    assertThat(resultado.getViewName(), equalTo("busqueda"));
    assertThat(resultado.getModel().get("texto"), equalTo("backend"));
  }

  @Test
  public void sugerenciasDevuelveCargosOrdenadosYSinDuplicados() throws Exception {
    mockMvc
      .perform(get("/vacantes/sugerencias"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()", is(7)))
      .andExpect(jsonPath("$[0]", is("Analista de Datos")))
      .andExpect(jsonPath("$[4]", is("Diseñador UX/UI")))
      .andExpect(jsonPath("$[6]", is("QA Tester")));
  }

  @Test
  public void sugerenciasFiltraSinDistinguirTildes() throws Exception {
    mockMvc
      .perform(get("/vacantes/sugerencias").param("q", "disen"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()", is(1)))
      .andExpect(jsonPath("$[0]", is("Diseñador UX/UI")));
  }

  @Test
  public void vacantesFiltraPorTextoZonaYModalidad() throws Exception {
    mockMvc
      .perform(
        get("/vacantes")
          .param("texto", "backend")
          .param("zona", "remota")
          .param("modalidad", "remota")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()", is(1)))
      .andExpect(jsonPath("$[0].titulo", is("Desarrollador Backend")))
      .andExpect(jsonPath("$[0].fechaPublicacion").isNotEmpty());
  }

  @Test
  public void vacantesFiltraPorSkillsYFecha() throws Exception {
    mockMvc
      .perform(get("/vacantes").param("skills", "java").param("fecha", "semana"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()", is(1)))
      .andExpect(jsonPath("$[0].titulo", is("Desarrollador Backend")));
  }
}
