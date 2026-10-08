package com.tallerwebi.infraestructura;

import com.fasterxml.jackson.databind.JsonNode;
import com.tallerwebi.dominio.VacanteDTO;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class JobicyMapper {

  public static final String FULL_TIME = "full-time";
  public static final String PART_TIME = "part-time";

  public VacanteDTO toDTO(JsonNode jsonNode) {
    VacanteDTO vacante = new VacanteDTO();
    vacante.setId(jsonNode.path("id").canConvertToLong() ? jsonNode.path("id").longValue() : null);
    vacante.setTitulo(texto(jsonNode, "jobTitle"));
    vacante.setEmpresa(texto(jsonNode, "companyName"));
    vacante.setDescripcion(texto(jsonNode, "jobDescription"));
    vacante.setUbicacion(texto(jsonNode, "jobGeo"));
    vacante.getCondiciones().setModalidad("remote");
    String nivel = jsonNode.path("jobLevel").asText("");
    vacante.getCondiciones().setSeniority(nivel.isBlank() ? null : nivel.toLowerCase(java.util.Locale.ROOT));
    vacante.getCondiciones().setJornada(jornada(jsonNode.path("jobType")));
    JsonNode sueldo = jsonNode.path("salaryMin");
    if ("monthly".equalsIgnoreCase(jsonNode.path("salaryPeriod").asText()) && sueldo.isNumber()) {
      vacante.getCondiciones().setSueldoMinimo(sueldo.decimalValue());
    }
    vacante.getCondiciones().setMoneda(jsonNode.path("salaryCurrency").asText(null));
    vacante.setUrl(urlSegura(jsonNode.path("url").asText("")));
    vacante.setFechaPublicacion(fecha(jsonNode.path("pubDate").asText("")));
    vacante.setFuente("JOBICY");
    return vacante;
  }

  private String texto(JsonNode jsonNode, String campo) {
    return Jsoup.parse(jsonNode.path(campo).asText("")).text();
  }

  private String jornada(JsonNode tipos) {
    for (JsonNode tipo : tipos) {
      if (FULL_TIME.equalsIgnoreCase(tipo.asText())) {
        return FULL_TIME;
      }
      if (PART_TIME.equalsIgnoreCase(tipo.asText())) {
        return PART_TIME;
      }
    }
    return null;
  }

  private String urlSegura(String valor) {
    return valor.startsWith("https://jobicy.com/") ? valor : null;
  }

  private LocalDateTime fecha(String valor) {
    try {
      return OffsetDateTime.parse(valor).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    } catch (DateTimeParseException excepcion) {
      return null;
    }
  }
}
