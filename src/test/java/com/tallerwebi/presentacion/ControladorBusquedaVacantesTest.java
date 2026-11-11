package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.ServicioCurriculumGenerado;
import com.tallerwebi.dominio.ServicioPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
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

  @Test
  public void filtroCvRechazaUsuarioSinPremium() throws Exception {
    ServicioPago servicioPago = Mockito.mock(ServicioPago.class);
    ServicioCurriculumGenerado servicioCurriculum = Mockito.mock(ServicioCurriculumGenerado.class);
    ControladorBusquedaVacantes controlador = new ControladorBusquedaVacantes(
      new CatalogoVacantesPrueba(),
      null,
      new com.tallerwebi.infraestructura.VacanteMapper(),
      servicioPago,
      servicioCurriculum
    );
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 10L);
    session.setAttribute("ROL", "CANDIDATO");
    Mockito.when(servicioPago.esPremiumActivo(10L)).thenReturn(false);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controlador).build();

    mvc
      .perform(get("/vacantes").param("usarCv", "true").session(session))
      .andExpect(status().isForbidden());
  }

  @Test
  public void filtroCvUsaElTextoAtsYOrdenaPorCoincidencia() throws Exception {
    ServicioPago servicioPago = Mockito.mock(ServicioPago.class);
    ServicioCurriculumGenerado servicioCurriculum = Mockito.mock(ServicioCurriculumGenerado.class);
    CurriculumGenerado curriculum = new CurriculumGenerado();
    curriculum.setTextoOriginal("Java Python");
    curriculum.setContenidoAts("Java Spring SQL");
    Mockito.when(servicioPago.esPremiumActivo(10L)).thenReturn(true);
    Mockito.when(servicioCurriculum.obtener(10L)).thenReturn(curriculum);
    ControladorBusquedaVacantes controlador = new ControladorBusquedaVacantes(
      new CatalogoVacantesPrueba(),
      null,
      new com.tallerwebi.infraestructura.VacanteMapper(),
      servicioPago,
      servicioCurriculum
    );
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 10L);
    session.setAttribute("ROL", "CANDIDATO");

    MockMvcBuilders
      .standaloneSetup(controlador)
      .build()
      .perform(get("/vacantes").param("usarCv", "true").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].porcentajeCoincidenciaCv", is(100)))
      .andExpect(jsonPath("$[0].palabrasCoincidentesCv", contains("java", "spring", "sql")));
  }
}
