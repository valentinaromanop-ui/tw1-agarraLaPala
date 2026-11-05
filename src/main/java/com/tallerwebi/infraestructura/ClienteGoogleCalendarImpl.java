package com.tallerwebi.infraestructura;

import com.fasterxml.jackson.databind.JsonNode;
import com.tallerwebi.config.GoogleCalendarConfig;
import com.tallerwebi.dominio.ClienteGoogleCalendar;
import com.tallerwebi.dominio.IntervaloHorario;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ClienteGoogleCalendarImpl implements ClienteGoogleCalendar {

  private final GoogleCalendarConfig configuracion;
  private final RestClient cliente;

  @Autowired
  public ClienteGoogleCalendarImpl(GoogleCalendarConfig configuracion) {
    this(configuracion, crearCliente());
  }

  public ClienteGoogleCalendarImpl(GoogleCalendarConfig configuracion, RestClient cliente) {
    this.configuracion = configuracion;
    this.cliente = cliente;
  }

  private static RestClient crearCliente() {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(10));
    fabrica.setReadTimeout(Duration.ofSeconds(10));
    return RestClient.builder().requestFactory(fabrica).build();
  }

  @Override
  public String urlAutorizacion(String state) {
    return UriComponentsBuilder
      .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
      .queryParam("client_id", configuracion.getClientId())
      .queryParam("redirect_uri", configuracion.getRedirectUri())
      .queryParam("response_type", "code")
      .queryParam("scope", SCOPES)
      .queryParam("access_type", "offline")
      .queryParam("prompt", "consent")
      .queryParam("state", state)
      .build()
      .encode()
      .toUriString();
  }

  @Override
  public String intercambiarCodigo(String codigo) {
    MultiValueMap<String, String> parametros = credenciales();
    parametros.add("grant_type", "authorization_code");
    parametros.add("code", codigo);
    parametros.add("redirect_uri", configuracion.getRedirectUri());
    JsonNode respuesta = solicitarToken(parametros);
    String permisos = respuesta.path("scope").asText("");
    if (!Arrays.asList(permisos.split(" ")).containsAll(Arrays.asList(SCOPES.split(" ")))) {
      throw new ConexionGoogleException();
    }
    return campoObligatorio(respuesta, "refresh_token");
  }

  @Override
  public String renovarAcceso(String refreshToken) {
    MultiValueMap<String, String> parametros = credenciales();
    parametros.add("grant_type", "refresh_token");
    parametros.add("refresh_token", refreshToken);
    return campoObligatorio(solicitarToken(parametros), "access_token");
  }

  @Override
  @SuppressWarnings("PMD.PreserveStackTrace")
  public List<IntervaloHorario> consultarPeriodosOcupados(
    String tokenAcceso,
    Instant desde,
    Instant hasta
  ) {
    Map<String, Object> consulta = Map.of(
      "timeMin",
      desde.toString(),
      "timeMax",
      hasta.toString(),
      "items",
      List.of(Map.of("id", "primary"))
    );
    try {
      JsonNode respuesta = cliente
        .post()
        .uri("https://www.googleapis.com/calendar/v3/freeBusy")
        .headers(headers -> headers.setBearerAuth(tokenAcceso))
        .contentType(MediaType.APPLICATION_JSON)
        .body(consulta)
        .retrieve()
        .body(JsonNode.class);
      return leerPeriodosOcupados(respuesta);
    } catch (RestClientException | DateTimeException | IllegalArgumentException excepcion) {
      throw new ConexionGoogleException();
    }
  }

  private List<IntervaloHorario> leerPeriodosOcupados(JsonNode respuesta) {
    if (respuesta == null) {
      throw new ConexionGoogleException();
    }
    JsonNode calendario = respuesta.path("calendars").path("primary");
    JsonNode ocupados = calendario.path("busy");

    if (!ocupados.isArray() || !calendario.path("errors").isEmpty()) {
      throw new ConexionGoogleException();
    }
    List<IntervaloHorario> periodos = new ArrayList<>();
    for (JsonNode ocupado : ocupados) {
      periodos.add(
        new IntervaloHorario(
          OffsetDateTime.parse(ocupado.path("start").asText()).toInstant(),
          OffsetDateTime.parse(ocupado.path("end").asText()).toInstant()
        )
      );
    }
    return periodos;
  }

  private MultiValueMap<String, String> credenciales() {
    MultiValueMap<String, String> parametros = new LinkedMultiValueMap<>();
    parametros.add("client_id", configuracion.getClientId());
    parametros.add("client_secret", configuracion.getClientSecret());
    return parametros;
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  private JsonNode solicitarToken(MultiValueMap<String, String> parametros) {
    try {
      JsonNode respuesta = cliente
        .post()
        .uri("https://oauth2.googleapis.com/token")
        .body(parametros)
        .retrieve()
        .body(JsonNode.class);
      if (respuesta == null) {
        throw new ConexionGoogleException();
      }
      return respuesta;
    } catch (RestClientException excepcion) {
      throw new ConexionGoogleException();
    }
  }

  private String campoObligatorio(JsonNode respuesta, String campo) {
    String valor = respuesta.path(campo).asText("");
    if (valor.isBlank()) {
      throw new ConexionGoogleException();
    }
    return valor;
  }
}
