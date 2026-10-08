package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioVacanteTest {

  private ServicioVacante servicioVacante;
  private VacanteProvider primerProviderMock;
  private VacanteProvider segundoProviderMock;
  private BusquedaVacanteDTO busqueda;

  @BeforeEach
  public void init() {
    primerProviderMock = mock(VacanteProvider.class);
    segundoProviderMock = mock(VacanteProvider.class);
    servicioVacante =
      new ServicioVacanteImpl(Arrays.asList(primerProviderMock, segundoProviderMock));
    busqueda = new BusquedaVacanteDTO();
    busqueda.setSkills(Arrays.asList("java", "sql"));
  }

  @Test
  public void buscarVacantesDeberiaConsultarLosProvidersYReunirSusOfertas() {
    VacanteDTO primera = vacante(null, "java");
    VacanteDTO segunda = vacante(null, "sql");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(List.of(primera));
    when(segundoProviderMock.buscarVacantes(any())).thenReturn(List.of(segunda));

    List<VacanteDTO> resultado = servicioVacante.obtenerVacantesPorSkills(busqueda);

    assertThat(resultado, equalTo(Arrays.asList(primera, segunda)));
    verify(primerProviderMock, times(1)).buscarVacantes(any());
    verify(segundoProviderMock, times(1)).buscarVacantes(any());
  }

  @Test
  public void deberiaPriorizarLaVacanteConMasSkillsAunqueSeaMasAntigua() {
    VacanteDTO reciente = vacante(LocalDateTime.of(2026, 9, 20, 0, 0), "java");
    VacanteDTO antigua = vacante(LocalDateTime.of(2026, 9, 1, 0, 0), "java", "sql");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(List.of(reciente));
    when(segundoProviderMock.buscarVacantes(any())).thenReturn(List.of(antigua));

    List<VacanteDTO> resultado = servicioVacante.obtenerVacantesPorSkills(busqueda);

    assertThat(resultado, equalTo(Arrays.asList(antigua, reciente)));
  }

  @Test
  public void conIgualCantidadDeSkillsDeberiaPriorizarLaVacanteMasReciente() {
    VacanteDTO antigua = vacante(LocalDateTime.of(2026, 9, 1, 0, 0), "java");
    VacanteDTO reciente = vacante(LocalDateTime.of(2026, 9, 20, 0, 0), "java");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(List.of(antigua));
    when(segundoProviderMock.buscarVacantes(any())).thenReturn(List.of(reciente));

    List<VacanteDTO> resultado = servicioVacante.obtenerVacantesPorSkills(busqueda);

    assertThat(resultado, equalTo(Arrays.asList(reciente, antigua)));
  }

  @Test
  public void conIgualCantidadDeSkillsDeberiaDejarLaVacanteSinFechaAlFinal() {
    VacanteDTO sinFecha = vacante(null, "java");
    VacanteDTO conFecha = vacante(LocalDateTime.of(2026, 9, 20, 0, 0), "java");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(List.of(sinFecha));
    when(segundoProviderMock.buscarVacantes(any())).thenReturn(List.of(conFecha));

    List<VacanteDTO> resultado = servicioVacante.obtenerVacantesPorSkills(busqueda);

    assertThat(resultado, equalTo(Arrays.asList(conFecha, sinFecha)));
  }

  @Test
  public void buscarSinSkillsDeberiaLanzarExcepcionSinConsultarProviders() {
    busqueda.setSkills(Collections.emptyList());

    assertThrows(
      BusquedaVacanteInvalida.class,
      () -> servicioVacante.obtenerVacantesPorSkills(busqueda)
    );
    verify(primerProviderMock, times(0)).buscarVacantes(any());
    verify(segundoProviderMock, times(0)).buscarVacantes(any());
  }

  private VacanteDTO vacante(LocalDateTime fecha, String... skills) {
    VacanteDTO vacante = new VacanteDTO();
    vacante.setFechaPublicacion(fecha);
    vacante.setSkills(Arrays.asList(skills));
    return vacante;
  }

  @Test
  public void deberiaAplicarTodosLosFiltrosYExcluirDatosAusentes() {
    VacanteDTO coincide = vacante(null, "java");
    coincide.getCondiciones().setModalidad("remote");
    coincide.getCondiciones().setSeniority("junior");
    coincide.getCondiciones().setJornada("full-time");
    VacanteDTO otraJornada = vacante(null, "java");
    otraJornada.getCondiciones().setModalidad("remote");
    otraJornada.getCondiciones().setSeniority("junior");
    otraJornada.getCondiciones().setJornada("part-time");
    VacanteDTO otroNivel = vacante(null, "java");
    otroNivel.getCondiciones().setModalidad("remote");
    otroNivel.getCondiciones().setSeniority("senior");
    otroNivel.getCondiciones().setJornada("full-time");
    VacanteDTO otraModalidad = vacante(null, "java");
    otraModalidad.getCondiciones().setModalidad("onsite");
    otraModalidad.getCondiciones().setSeniority("junior");
    otraModalidad.getCondiciones().setJornada("full-time");
    VacanteDTO sinDatos = vacante(null, "java");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(Arrays.asList(coincide, otraJornada, otroNivel, otraModalidad, sinDatos));
    busqueda.getCondiciones().setModalidad("remote");
    busqueda.getCondiciones().setSeniority("junior");
    busqueda.getCondiciones().setJornada("full-time");

    assertThat(servicioVacante.obtenerVacantesPorSkills(busqueda), equalTo(List.of(coincide)));
  }

  @Test
  public void quitarFiltrosDeberiaIncluirVacantesSinDatos() {
    VacanteDTO sinDatos = vacante(null, "java");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(List.of(sinDatos));
    busqueda.getCondiciones().setModalidad("");
    busqueda.getCondiciones().setSeniority("");
    busqueda.getCondiciones().setJornada("");

    assertThat(servicioVacante.obtenerVacantesPorSkills(busqueda), equalTo(List.of(sinDatos)));
  }

  @Test
  public void deberiaCompararSueldosMensualesSoloConIgualMoneda() {
    VacanteDTO coincide = vacanteConSueldo("1000", "USD");
    VacanteDTO menor = vacanteConSueldo("900", "USD");
    VacanteDTO otraMoneda = vacanteConSueldo("1000000", "ARS");
    VacanteDTO sinDatos = vacante(null, "java");
    when(primerProviderMock.buscarVacantes(any())).thenReturn(Arrays.asList(coincide, menor, otraMoneda, sinDatos));
    busqueda.getCondiciones().setSueldoMinimo(new BigDecimal("1000"));
    busqueda.getCondiciones().setMoneda("USD");

    assertThat(servicioVacante.obtenerVacantesPorSkills(busqueda), equalTo(List.of(coincide)));
  }

  private VacanteDTO vacanteConSueldo(String sueldo, String moneda) {
    VacanteDTO vacante = vacante(null, "java");
    vacante.getCondiciones().setSueldoMinimo(new BigDecimal(sueldo));
    vacante.getCondiciones().setMoneda(moneda);

    return vacante;
  }
}
