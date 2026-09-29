package com.tallerwebi.infraestructura;

import com.fasterxml.jackson.databind.JsonNode;
import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.VacanteProvider;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JobicyVacanteProvider implements VacanteProvider {

  private final RestClient restClient;
  private final JobicyMapper jobicyMapper;

  @Autowired
  public JobicyVacanteProvider(JobicyMapper jobicyMapper) {
    this(crearCliente(), jobicyMapper);
  }

  public JobicyVacanteProvider(RestClient restClient, JobicyMapper jobicyMapper) {
    this.restClient = restClient;
    this.jobicyMapper = jobicyMapper;
  }

  private static RestClient crearCliente() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(10000);
    return RestClient.builder().baseUrl("https://jobicy.com").requestFactory(factory).build();
  }

  @Override
  public List<VacanteDTO> buscarVacantes(BusquedaVacanteDTO busqueda) {
    Map<Long, VacanteDTO> vacantes = new LinkedHashMap<>();
    for (String skill : busqueda.getSkills()) {
      JsonNode jobs = obtenerPublicaciones(skill);
      for (JsonNode job : jobs) {
        VacanteDTO vacante = jobicyMapper.toDTO(job);
        Long id = vacante.getId();
        if (!vacantes.containsKey(id)) {
          vacante.setSkills(new ArrayList<>());
          vacantes.put(id, vacante);
        }
        List<String> skills = vacantes.get(id).getSkills();
        if (!skills.contains(skill)) {
          skills.add(skill);
        }
      }
    }
    return new ArrayList<>(vacantes.values());
  }

  private JsonNode obtenerPublicaciones(String skill) {
    try {
      JsonNode respuesta = restClient
        .get()
        .uri("/api/v2/remote-jobs?count=200&industry=engineering&tag={skill}", skill)
        .retrieve()
        .body(JsonNode.class);
      if (respuesta == null || !respuesta.path("jobs").isArray()) {
        throw new FuenteVacanteNoDisponible("Jobicy", null);
      }
      return respuesta.get("jobs");
    } catch (RestClientException excepcion) {
      throw new FuenteVacanteNoDisponible("Jobicy", excepcion);
    }
  }
}
