package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.tallerwebi.dominio.ClienteGoogleCalendar;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public class ClienteGoogleCalendarTest {

  private MockRestServiceServer servidor;
  private ClienteGoogleCalendarImpl cliente;
  private ConfiguracionGoogleCalendar configuracion;

  @BeforeEach
  public void preparar() {
    MockEnvironment entorno = new MockEnvironment()
      .withProperty("google.calendar.client-id", "cliente")
      .withProperty("google.calendar.client-secret", "secreto")
      .withProperty(
        "google.calendar.redirect-uri",
        "http://localhost:8080/spring/google-calendar/callback"
      );
    configuracion = new ConfiguracionGoogleCalendar(entorno);
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
