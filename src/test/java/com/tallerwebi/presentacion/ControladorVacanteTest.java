package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.ServicioVacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class ControladorVacanteTest {

  private ControladorVacante controladorVacante;
  private ServicioVacante servicioVacanteMock;

  @BeforeEach
  public void init() {
    servicioVacanteMock = mock(ServicioVacante.class);
    controladorVacante =
      new ControladorVacante(
        servicioVacanteMock,
        mock(com.tallerwebi.dominio.ServicioPostulacion.class)
      );
  }

  @Test
  public void mostrarRecomendadasDeberiaBuscarConSkillsFijas() {
    List<VacanteDTO> vacantes = Collections.singletonList(new VacanteDTO());
    when(servicioVacanteMock.obtenerVacantesPorSkills(any())).thenReturn(vacantes);

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas(
      new org.springframework.mock.web.MockHttpSession()
    );

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(modelAndView.getModel().get("vacantes"), equalTo(vacantes));
    verify(servicioVacanteMock, times(1)).obtenerVacantesPorSkills(any());
  }

  @Test
  public void deberiaBuscarInicialmenteSoloPorSkills() {
    ModelAndView vista = controladorVacante.mostrarVacantesRecomendadas(
      new org.springframework.mock.web.MockHttpSession()
    );
    BusquedaVacanteDTO busqueda = (BusquedaVacanteDTO) vista.getModel().get("busqueda");

    org.junit.jupiter.api.Assertions.assertNull(busqueda.getModalidad());
    org.junit.jupiter.api.Assertions.assertNull(busqueda.getSeniority());
    org.junit.jupiter.api.Assertions.assertNull(busqueda.getJornada());
    org.junit.jupiter.api.Assertions.assertNull(busqueda.getSueldoMinimo());
    org.junit.jupiter.api.Assertions.assertNull(busqueda.getMoneda());
    assertThat(busqueda.getSkills(), equalTo(Arrays.asList("Java", "SQL", "PHP")));
  }

  @Test
  public void deberiaRespetarFiltrosVaciosSinReponerPreferencias() {
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();
    busqueda.setModalidad("");
    busqueda.setSeniority("");
    busqueda.setJornada("");

    controladorVacante.filtrarVacantes(
      busqueda,
      new BeanPropertyBindingResult(busqueda, "busqueda"),
      new org.springframework.mock.web.MockHttpSession()
    );

    assertThat(busqueda.getModalidad(), equalTo(""));
    assertThat(busqueda.getSeniority(), equalTo(""));
    assertThat(busqueda.getJornada(), equalTo(""));
    assertThat(busqueda.getSkills(), equalTo(Arrays.asList("Java", "SQL", "PHP")));
    verify(servicioVacanteMock).obtenerVacantesPorSkills(busqueda);
  }

  @Test
  public void deberiaRecibirFiltrosDelFormulario() throws Exception {
    ModelAndView vista = MockMvcBuilders
      .standaloneSetup(controladorVacante)
      .build()
      .perform(
        get("/vacantes/filtrar")
          .param("modalidad", "hybrid")
          .param("jornada", "part-time")
          .param("seniority", "senior")
          .param("sueldoMinimo", "1500")
          .param("moneda", "USD")
      )
      .andReturn()
      .getModelAndView();
    BusquedaVacanteDTO busqueda = (BusquedaVacanteDTO) vista.getModel().get("busqueda");

    assertThat(busqueda.getModalidad(), equalTo("hybrid"));
    assertThat(busqueda.getJornada(), equalTo("part-time"));
    assertThat(busqueda.getSeniority(), equalTo("senior"));
    assertThat(busqueda.getSueldoMinimo(), equalTo(new BigDecimal("1500")));
    verify(servicioVacanteMock).obtenerVacantesPorSkills(busqueda);
  }

  @Test
  public void sueldoInvalidoDeberiaMostrarErrorSinBuscar() throws Exception {
    ModelAndView vista = MockMvcBuilders
      .standaloneSetup(controladorVacante)
      .build()
      .perform(get("/vacantes/filtrar").param("sueldoMinimo", "texto"))
      .andReturn()
      .getModelAndView();

    assertThat(
      vista.getModel().get("error"),
      equalTo("Ingresá un sueldo mínimo válido, mayor o igual a cero.")
    );
    verify(servicioVacanteMock, times(0)).obtenerVacantesPorSkills(any());
  }

  @Test
  public void deberiaRenderizarLosFiltrosYLasVacantesConThymeleaf() throws Exception {
    FileTemplateResolver templates = new FileTemplateResolver();
    templates.setPrefix("src/main/webapp/WEB-INF/views/thymeleaf/");
    templates.setSuffix(".html");
    templates.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(templates);
    ThymeleafViewResolver vistas = new ThymeleafViewResolver();
    vistas.setTemplateEngine(engine);
    vistas.setCharacterEncoding("UTF-8");
    VacanteDTO vacante = new VacanteDTO();
    vacante.setTitulo("Desarrollador Java");
    vacante.setSkills(List.of("java"));
    when(servicioVacanteMock.obtenerVacantesPorSkills(any())).thenReturn(List.of(vacante));

    String html = MockMvcBuilders
      .standaloneSetup(controladorVacante)
      .setViewResolvers(vistas)
      .build()
      .perform(get("/vacantes/recomendadas"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertTrue(html.contains("Desarrollador Java"));
    assertTrue(html.contains("Sueldo mensual no informado"));
    assertThat(
      org.jsoup.Jsoup.parse(html).select("#jornada option[value=full-time]").text(),
      equalTo("Tiempo completo")
    );
  }

  @Test
  public void buscarSinResultadosDeberiaMostrarUnaListaVacia() {
    List<VacanteDTO> vacantes = Collections.emptyList();
    when(servicioVacanteMock.obtenerVacantesPorSkills(any())).thenReturn(vacantes);

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas(
      new org.springframework.mock.web.MockHttpSession()
    );

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(modelAndView.getModel().get("vacantes"), equalTo(vacantes));
  }

  @Test
  public void buscarSinSkillsDeberiaMostrarError() {
    when(servicioVacanteMock.obtenerVacantesPorSkills(any()))
      .thenThrow(new BusquedaVacanteInvalida());

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas(
      new org.springframework.mock.web.MockHttpSession()
    );

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Ingresá al menos una skill para buscar vacantes.")
    );
  }

  @Test
  public void buscarSiLaFuenteFallaDeberiaMostrarError() {
    when(servicioVacanteMock.obtenerVacantesPorSkills(any()))
      .thenThrow(new FuenteVacanteNoDisponible("Jobicy", new IllegalStateException()));

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas(
      new org.springframework.mock.web.MockHttpSession()
    );

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("No pudimos consultar Jobicy. Intentá nuevamente más tarde.")
    );
  }
}
