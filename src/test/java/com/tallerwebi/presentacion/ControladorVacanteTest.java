package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioVacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorVacanteTest {

  private ControladorVacante controladorVacante;
  private ServicioVacante servicioVacanteMock;

  @BeforeEach
  public void init() {
    servicioVacanteMock = mock(ServicioVacante.class);
    controladorVacante = new ControladorVacante(servicioVacanteMock);
  }

  @Test
  public void mostrarRecomendadasDeberiaBuscarConSkillsFijas() {
    List<VacanteDTO> vacantes = Collections.singletonList(new VacanteDTO());
    when(servicioVacanteMock.obtenerVacantesPorSkills(any())).thenReturn(vacantes);

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas();

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(modelAndView.getModel().get("vacantes"), equalTo(vacantes));
    verify(servicioVacanteMock, times(1)).obtenerVacantesPorSkills(any());
  }

  @Test
  public void buscarSinResultadosDeberiaMostrarUnaListaVacia() {
    List<VacanteDTO> vacantes = Collections.emptyList();
    when(servicioVacanteMock.obtenerVacantesPorSkills(any())).thenReturn(vacantes);

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas();

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(modelAndView.getModel().get("vacantes"), equalTo(vacantes));
  }

  @Test
  public void buscarSinSkillsDeberiaMostrarError() {
    when(servicioVacanteMock.obtenerVacantesPorSkills(any()))
      .thenThrow(new BusquedaVacanteInvalida());

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas();

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

    ModelAndView modelAndView = controladorVacante.mostrarVacantesRecomendadas();

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("vacantes"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("No pudimos consultar Jobicy. Intentá nuevamente más tarde.")
    );
  }
}
