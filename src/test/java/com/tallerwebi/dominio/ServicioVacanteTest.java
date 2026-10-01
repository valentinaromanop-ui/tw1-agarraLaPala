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
}
