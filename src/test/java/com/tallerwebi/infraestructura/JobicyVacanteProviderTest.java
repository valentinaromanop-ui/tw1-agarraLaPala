package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class JobicyVacanteProviderTest {

  private MockRestServiceServer servidor;
  private JobicyVacanteProvider provider;

  @BeforeEach
  public void preparar() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://jobicy.com");
    servidor = MockRestServiceServer.bindTo(builder).build();
    provider = new JobicyVacanteProvider(builder.build(), new JobicyMapper());
  }

  @Test
  public void devuelveUnaVacanteDeJobicy() {
    responder("java", "{\"jobs\":[{\"id\":1,\"jobTitle\":\"Java Developer\"}]}");

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda("java"));

    assertEquals(1, resultado.size());
    assertEquals("Java Developer", resultado.getFirst().getTitulo());
    servidor.verify();
  }

  @Test
  public void consultaCadaSkillYUneLasVacantesSinRepetirlas() {
    responder("java", "{\"jobs\":[{\"id\":1},{\"id\":2}]}");
    responder("sql", "{\"jobs\":[{\"id\":1},{\"id\":3}]}");

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda("java", "sql"));

    assertEquals(3, resultado.size());
    assertEquals(Arrays.asList("java", "sql"), resultado.getFirst().getSkills());
    servidor.verify();
  }

  @Test
  public void devuelveListaVaciaCuandoNoHayOfertas() {
    responder("java", "{\"jobs\":[]}");

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda("java"));

    assertTrue(resultado.isEmpty());
    servidor.verify();
  }

  @Test
  public void lanzaExcepcionCuandoJobicyFalla() {
    servidor.expect(requestTo(url("java"))).andRespond(withServerError());

    assertThrows(FuenteVacanteNoDisponible.class, () -> provider.buscarVacantes(busqueda("java")));
    servidor.verify();
  }

  private void responder(String skill, String json) {
    servidor
      .expect(requestTo(url(skill)))
      .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
  }

  private String url(String skill) {
    return "https://jobicy.com/api/v2/remote-jobs?count=200&industry=engineering&tag=" + skill;
  }

  private BusquedaVacanteDTO busqueda(String... skills) {
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();
    busqueda.setSkills(Arrays.asList(skills));
    return busqueda;
  }
}
