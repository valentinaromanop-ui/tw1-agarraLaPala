package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.tallerwebi.config.GoogleCalendarConfig;
import com.tallerwebi.dominio.ClienteGoogleCalendar;
import com.tallerwebi.dominio.EventoGoogleCalendar;
import com.tallerwebi.dominio.IntervaloHorario;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class ClienteGoogleCalendarTest {

  private static final Instant DESDE = Instant.parse("2026-11-02T11:00:00Z");
  private static final Instant HASTA = Instant.parse("2026-11-16T03:00:00Z");

  private MockRestServiceServer servidor;
  private ClienteGoogleCalendarImpl cliente;
  private GoogleCalendarConfig configuracion;

  @BeforeEach
  public void preparar() {
    MockEnvironment entorno = new MockEnvironment()
      .withProperty("google.calendar.client-id", "cliente")
      .withProperty("google.calendar.client-secret", "secreto")
      .withProperty(
        "google.calendar.redirect-uri",
        "http://localhost:8080/spring/google-calendar/callback"
      );
    configuracion = new GoogleCalendarConfig(entorno);
    RestClient.Builder builder = RestClient.builder();
    servidor = MockRestServiceServer.bindTo(builder).build();
    cliente = new ClienteGoogleCalendarImpl(configuracion, builder.build());
  }

  @Test
  public void solicitaAccesoOfflineYState() {
    // preparación
    String state = "estado";

    // ejecución
    String url = cliente.urlAutorizacion(state);

    // validación
    assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
    assertTrue(url.contains("access_type=offline"));
    assertTrue(url.contains("prompt=consent"));
    assertTrue(url.contains("state=estado"));
  }

  @Test
  public void intercambiaCodigoConLosPermisosRequeridos() {
    // preparación
    servidor
      .expect(requestTo("https://oauth2.googleapis.com/token"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
      .andExpect(
        content().string(org.hamcrest.Matchers.containsString("grant_type=authorization_code"))
      )
      .andExpect(content().string(org.hamcrest.Matchers.containsString("code=codigo")))
      .andRespond(
        withSuccess(respuestaConPermisos(ClienteGoogleCalendar.SCOPES), MediaType.APPLICATION_JSON)
      );

    // ejecución
    String tokenObtenido = cliente.intercambiarCodigo("codigo");

    // validación
    assertEquals("renovacion", tokenObtenido);
    servidor.verify();
  }

  @Test
  public void rechazaPermisosInsuficientes() {
    // preparación
    dadoQueGoogleResponde(respuestaConPermisos("openid"));

    // ejecución - validación
    assertThrows(ConexionGoogleException.class, () -> cliente.intercambiarCodigo("codigo"));
    servidor.verify();
  }

  @Test
  public void rechazaRespuestaSinRefreshToken() {
    // preparación
    servidor
      .expect(anything())
      .andRespond(
        withSuccess(
          "{\"scope\":\"" + ClienteGoogleCalendar.SCOPES + "\"}",
          MediaType.APPLICATION_JSON
        )
      );

    // ejecución y validación
    assertThrows(ConexionGoogleException.class, () -> cliente.intercambiarCodigo("codigo"));
    servidor.verify();
  }

  @Test
  public void renuevaAcceso() {
    // preparación
    servidor
      .expect(requestTo("https://oauth2.googleapis.com/token"))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=refresh_token")))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("refresh_token=renovacion")))
      .andRespond(withSuccess("{\"access_token\":\"acceso\"}", MediaType.APPLICATION_JSON));

    // ejecución
    String tokenObtenido = cliente.renovarAcceso("renovacion");

    // validación
    assertEquals("acceso", tokenObtenido);
    servidor.verify();
  }

  @Test
  public void noExponeElCuerpoDelErrorDeGoogle() {
    // preparación
    servidor.expect(anything()).andRespond(withBadRequest().body("dato-sensible"));

    // ejecución y validación de la excepción
    ConexionGoogleException error = assertThrows(
      ConexionGoogleException.class,
      () -> cliente.renovarAcceso("revocado")
    );

    // validación del contenido del error
    assertFalse(error.toString().contains("dato-sensible"));
    assertNull(error.getCause());
    servidor.verify();
  }

  @Test
  public void deberiaConsultarElCalendarioPrincipalConElTokenYElPeriodoSolicitado() {
    // preparacion
    servidor
      .expect(requestTo("https://www.googleapis.com/calendar/v3/freeBusy"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(header("Authorization", "Bearer acceso"))
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.timeMin").value(DESDE.toString()))
      .andExpect(jsonPath("$.timeMax").value(HASTA.toString()))
      .andExpect(jsonPath("$.items[0].id").value("primary"))
      .andRespond(
        withSuccess(
          """
          {
            "calendars": {
              "primary": {
                "busy": [
                  {"start": "2026-11-02T09:15:00-03:00", "end": "2026-11-02T10:00:00-03:00"}
                ]
              }
            }
          }
          """,
          MediaType.APPLICATION_JSON
        )
      );

    // ejecucion
    List<IntervaloHorario> ocupados = cuandoConsultoLosPeriodosOcupados();

    // validacion
    assertThat(ocupados.size(), equalTo(1));
    assertThat(ocupados.getFirst().getInicio(), equalTo(Instant.parse("2026-11-02T12:15:00Z")));
    assertThat(ocupados.getFirst().getFin(), equalTo(Instant.parse("2026-11-02T13:00:00Z")));
    servidor.verify();
  }

  @Test
  public void deberiaDevolverUnaListaVaciaSiGoogleNoTienePeriodosOcupados() {
    // preparacion
    dadoQueGoogleRespondeDisponibilidad(
      """
      {"busy": []}
      """
    );

    // ejecucion
    List<IntervaloHorario> ocupados = cuandoConsultoLosPeriodosOcupados();

    // validacion
    assertThat(ocupados.isEmpty(), equalTo(true));
    servidor.verify();
  }

  @Test
  public void deberiaRechazarUnErrorDelCalendarioAunqueGoogleRespondaHttp200() {
    // preparacion
    dadoQueGoogleRespondeDisponibilidad(
      """
      {"errors": [{"reason": "internalError"}], "busy": []}
      """
    );

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoConsultoLosPeriodosOcupados());
    servidor.verify();
  }

  @Test
  public void deberiaRechazarUnaRespuestaSinElCalendarioPrincipal() {
    // preparacion
    servidor
      .expect(requestTo("https://www.googleapis.com/calendar/v3/freeBusy"))
      .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoConsultoLosPeriodosOcupados());
    servidor.verify();
  }

  @Test
  public void deberiaRechazarUnaRespuestaSinLosPeriodosOcupados() {
    // preparacion
    dadoQueGoogleRespondeDisponibilidad("{}");

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoConsultoLosPeriodosOcupados());
    servidor.verify();
  }

  @Test
  public void deberiaRechazarPeriodosConFechasInvalidas() {
    // preparacion
    dadoQueGoogleRespondeDisponibilidad(
      """
      {"busy": [{"start": "fecha-invalida", "end": "2026-11-02T13:00:00Z"}]}
      """
    );

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoConsultoLosPeriodosOcupados());
    servidor.verify();
  }

  @Test
  public void deberiaRechazarPeriodosConFinAnteriorAlInicio() {
    // preparacion
    dadoQueGoogleRespondeDisponibilidad(
      """
      {"busy": [{"start": "2026-11-02T14:00:00Z", "end": "2026-11-02T13:00:00Z"}]}
      """
    );

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoConsultoLosPeriodosOcupados());
    servidor.verify();
  }

  @Test
  public void deberiaDevolverUnErrorSiFallaLaConsultaHttpDeDisponibilidad() {
    // preparacion
    servidor
      .expect(requestTo("https://www.googleapis.com/calendar/v3/freeBusy"))
      .andRespond(withServerError());

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoConsultoLosPeriodosOcupados());
    servidor.verify();
  }

  @Test
  public void deberiaCrearElEventoConMeetYNotificarAlCandidato() {
    // preparacion
    servidor
      .expect(
        requestTo(
          "https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1&sendUpdates=all"
        )
      )
      .andExpect(method(HttpMethod.POST))
      .andExpect(header("Authorization", "Bearer acceso"))
      .andExpect(jsonPath("$.id").value("entrevista123"))
      .andExpect(jsonPath("$.start.dateTime").value("2026-11-02T12:00:00Z"))
      .andExpect(jsonPath("$.end.dateTime").value("2026-11-02T12:30:00Z"))
      .andExpect(jsonPath("$.attendees[0].email").value("candidato@test.com"))
      .andExpect(jsonPath("$.conferenceData.createRequest.requestId").value("entrevista123"))
      .andExpect(
        jsonPath("$.conferenceData.createRequest.conferenceSolutionKey.type").value("hangoutsMeet")
      )
      .andRespond(
        withSuccess(
          """
          {"id": "entrevista123", "hangoutLink": "https://meet.google.com/abc-defg-hij"}
          """,
          MediaType.APPLICATION_JSON
        )
      );

    // ejecucion
    EventoGoogleCalendar evento = cuandoCreoElEvento();

    // validacion
    assertThat(evento.getId(), equalTo("entrevista123"));
    assertThat(evento.getUrlMeet(), equalTo("https://meet.google.com/abc-defg-hij"));
    servidor.verify();
  }

  @Test
  public void deberiaAceptarElEventoCuandoMeetTodaviaEstaPendiente() {
    // preparacion
    servidor
      .expect(anything())
      .andRespond(
        withSuccess(
          """
          {"id": "entrevista123", "conferenceData": {"createRequest": {"status": {"statusCode": "pending"}}}}
          """,
          MediaType.APPLICATION_JSON
        )
      );

    // ejecucion
    EventoGoogleCalendar evento = cuandoCreoElEvento();

    // validacion
    assertThat(evento.getId(), equalTo("entrevista123"));
    assertThat(evento.getUrlMeet(), equalTo(null));
    servidor.verify();
  }

  @Test
  public void deberiaConsultarElMeetDeUnEventoYaCreado() {
    // preparacion
    servidor
      .expect(
        requestTo("https://www.googleapis.com/calendar/v3/calendars/primary/events/entrevista123")
      )
      .andExpect(method(HttpMethod.GET))
      .andExpect(header("Authorization", "Bearer acceso"))
      .andRespond(
        withSuccess(
          """
          {"id": "entrevista123", "conferenceData": {"entryPoints": [
            {"entryPointType": "phone", "uri": "tel:123"},
            {"entryPointType": "video", "uri": "https://meet.google.com/abc-defg-hij"}
          ]}}
          """,
          MediaType.APPLICATION_JSON
        )
      );

    // ejecucion
    String enlace = cliente.obtenerEnlaceMeet("acceso", "entrevista123");

    // validacion
    assertThat(enlace, equalTo("https://meet.google.com/abc-defg-hij"));
    servidor.verify();
  }

  @Test
  public void deberiaRechazarUnaRespuestaSinElIdentificadorDelEvento() {
    // preparacion
    servidor.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoCreoElEvento());
    servidor.verify();
  }

  @Test
  public void deberiaRechazarUnEnlaceQueNoSeaDeGoogleMeet() {
    // preparacion
    servidor
      .expect(anything())
      .andRespond(
        withSuccess(
          """
          {"id": "entrevista123", "hangoutLink": "javascript:alert(1)"}
          """,
          MediaType.APPLICATION_JSON
        )
      );

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoCreoElEvento());
    servidor.verify();
  }

  @Test
  public void noDeberiaReintentarLaCreacionSiGoogleDevuelveUnConflicto() {
    // preparacion
    servidor.expect(anything()).andRespond(withStatus(HttpStatus.CONFLICT));

    // ejecucion y validacion
    assertThrows(ConexionGoogleException.class, () -> cuandoCreoElEvento());
    servidor.verify();
  }

  @Test
  public void deberiaInformarSiGoogleNoPermiteCrearElEvento() {
    // preparacion
    servidor.expect(anything()).andRespond(withBadRequest().body("Error privado de Google"));

    // ejecucion y validacion
    ConexionGoogleException error = assertThrows(
      ConexionGoogleException.class,
      () -> cuandoCreoElEvento()
    );
    assertFalse(error.toString().contains("Error privado de Google"));
    servidor.verify();
  }

  private EventoGoogleCalendar cuandoCreoElEvento() {
    return cliente.crearEventoConMeet(
      "acceso",
      "entrevista123",
      "Entrevista laboral",
      "candidato@test.com",
      new IntervaloHorario(
        Instant.parse("2026-11-02T12:00:00Z"),
        Instant.parse("2026-11-02T12:30:00Z")
      )
    );
  }

  private List<IntervaloHorario> cuandoConsultoLosPeriodosOcupados() {
    return cliente.consultarPeriodosOcupados("acceso", DESDE, HASTA);
  }

  private void dadoQueGoogleRespondeDisponibilidad(String calendario) {
    servidor
      .expect(requestTo("https://www.googleapis.com/calendar/v3/freeBusy"))
      .andRespond(
        withSuccess(
          """
          {"calendars": {"primary": %s}}
          """.formatted(calendario),
          MediaType.APPLICATION_JSON
        )
      );
  }

  private void dadoQueGoogleResponde(String respuesta) {
    servidor
      .expect(requestTo("https://oauth2.googleapis.com/token"))
      .andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
  }

  private String respuestaConPermisos(String permisos) {
    return """
    {
      "refresh_token": "renovacion",
      "scope": "%s"
    }
    """.formatted(permisos);
  }
}
