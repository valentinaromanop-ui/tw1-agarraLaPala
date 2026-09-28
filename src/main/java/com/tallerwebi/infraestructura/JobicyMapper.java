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

  public VacanteDTO toDTO(JsonNode jsonNode) {
    VacanteDTO vacante = new VacanteDTO();
    vacante.setId(jsonNode.path("id").canConvertToLong() ? jsonNode.path("id").longValue() : null);
    vacante.setTitulo(texto(jsonNode, "jobTitle"));
    vacante.setEmpresa(texto(jsonNode, "companyName"));
    vacante.setDescripcion(texto(jsonNode, "jobDescription"));
    vacante.setUbicacion(texto(jsonNode, "jobGeo"));
    vacante.setModalidad("REMOTE");
    vacante.setUrl(urlSegura(jsonNode.path("url").asText("")));
    vacante.setFechaPublicacion(fecha(jsonNode.path("pubDate").asText("")));
    vacante.setFuente("JOBICY");
    return vacante;
  }

  private String texto(JsonNode jsonNode, String campo) {
    return Jsoup.parse(jsonNode.path(campo).asText("")).text();
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
