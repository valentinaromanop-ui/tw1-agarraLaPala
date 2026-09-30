package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class JobicyVacanteProviderTest {

  private static final String URL = "https://jobicy.com/api/v2/remote-jobs?count=200&industry=engineering&tag={skill}";
  private RestTemplate restTemplateMock;
  private JobicyVacanteProvider provider;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void preparar() {
    restTemplateMock = mock(RestTemplate.class);
    provider = new JobicyVacanteProvider(restTemplateMock, new JobicyMapper());
    objectMapper = new ObjectMapper();
  }

  @Test
  public void devuelveUnaVacanteDeJobicy() throws Exception {
    JsonNode respuesta = objectMapper.readTree("{\"jobs\":[{\"id\":1,\"jobTitle\":\"Java Developer\"}]}");
    when(restTemplateMock.getForObject(URL, JsonNode.class, "java")).thenReturn(respuesta);

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda("java"));

    assertEquals(1, resultado.size());
    assertEquals("Java Developer", resultado.getFirst().getTitulo());
  }

  @Test
  public void consultaCadaSkillYUneLasVacantesSinRepetirlas() throws Exception {
    JsonNode respuestaJava = objectMapper.readTree("{\"jobs\":[{\"id\":1},{\"id\":2}]}");
    JsonNode respuestaSql = objectMapper.readTree("{\"jobs\":[{\"id\":1},{\"id\":3}]}");
    when(restTemplateMock.getForObject(URL, JsonNode.class, "java")).thenReturn(respuestaJava);
    when(restTemplateMock.getForObject(URL, JsonNode.class, "sql")).thenReturn(respuestaSql);

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda("java", "sql"));

    assertEquals(3, resultado.size());
    assertEquals(Arrays.asList("java", "sql"), resultado.getFirst().getSkills());
    verify(restTemplateMock).getForObject(URL, JsonNode.class, "java");
    verify(restTemplateMock).getForObject(URL, JsonNode.class, "sql");
  }

  @Test
  public void devuelveListaVaciaCuandoNoHayOfertas() throws Exception {
    JsonNode respuesta = objectMapper.readTree("{\"jobs\":[]}");
    when(restTemplateMock.getForObject(URL, JsonNode.class, "java")).thenReturn(respuesta);

    List<VacanteDTO> resultado = provider.buscarVacantes(busqueda("java"));

    assertTrue(resultado.isEmpty());
  }

  @Test
  public void lanzaExcepcionCuandoJobicyFalla() {
    when(restTemplateMock.getForObject(URL, JsonNode.class, "java")).thenThrow(new RestClientException("Error al consultar Jobicy"));

    assertThrows(FuenteVacanteNoDisponible.class, () -> provider.buscarVacantes(busqueda("java")));
  }

  private BusquedaVacanteDTO busqueda(String... skills) {
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();
    busqueda.setSkills(Arrays.asList(skills));
    return busqueda;
  }
}
