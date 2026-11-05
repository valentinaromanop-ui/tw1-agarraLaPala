package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.ServicioChat;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControladorChatTest {

  private ServicioChat servicioChat;
  private ControladorChat controlador;
  private MockHttpServletRequest request;
  private MockMvc mockMvc;
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    servicioChat = mock(ServicioChat.class);
    controlador = new ControladorChat(servicioChat);
    request = new MockHttpServletRequest();
    mockMvc = MockMvcBuilders.standaloneSetup(controlador).build();
    session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 7L);
  }

  @Test
  void listarSinSesionDevuelve401() {
    ResponseEntity<?> respuesta = controlador.conversaciones(request);

    assertThat(respuesta.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    verifyNoInteractions(servicioChat);
  }

  @Test
  void marcarLeidosSinSesionDevuelve401() {
    ResponseEntity<?> respuesta = controlador.marcarLeidos(1L, request);

    assertThat(respuesta.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    verifyNoInteractions(servicioChat);
  }

  @Test
  void listarAutenticadoUsaElUsuarioDeLaSesion() {
    HttpSession session = request.getSession();
    session.setAttribute("USUARIO_ID", 7L);
    when(servicioChat.listarConversaciones(7L)).thenReturn(List.of());

    ResponseEntity<?> respuesta = controlador.conversaciones(request);

    assertThat(respuesta.getStatusCode(), equalTo(HttpStatus.OK));
    verify(servicioChat).listarConversaciones(7L);
  }

  @Test
  void conversacionesConMockMvcDevuelve200() throws Exception {
    when(servicioChat.listarConversaciones(7L)).thenReturn(List.of());

    mockMvc
      .perform(get("/api/chat/conversaciones").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray());
  }

  @Test
  void conversacionesSinSesionConMockMvcDevuelve401() throws Exception {
    mockMvc.perform(get("/api/chat/conversaciones")).andExpect(status().isUnauthorized());
  }

  @Test
  void conversacionesDevuelveJsonConHoraString() throws Exception {
    Postulacion postulacion = new Postulacion();
    postulacion.setId(52L);
    postulacion.setTitulo("Desarrollador");
    postulacion.setFechaPostulacion(LocalDateTime.of(2026, 11, 4, 18, 30));
    when(servicioChat.listarConversaciones(7L)).thenReturn(List.of(postulacion));
    when(servicioChat.listarMensajes(7L, 52L)).thenReturn(List.of());

    mockMvc
      .perform(get("/api/chat/conversaciones").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(52))
      .andExpect(jsonPath("$[0].hora").isString());
  }

  @Test
  void mensajesConMockMvcDevuelve200() throws Exception {
    Mensaje mensaje = mensaje(15L, 7L, "Hola");
    when(servicioChat.listarMensajes(7L, 52L)).thenReturn(List.of(mensaje));

    mockMvc
      .perform(get("/api/chat/conversaciones/52").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(15))
      .andExpect(jsonPath("$[0].contenido").value("Hola"));
  }

  @Test
  void mensajesSinSesionConMockMvcDevuelve401() throws Exception {
    mockMvc.perform(get("/api/chat/conversaciones/52")).andExpect(status().isUnauthorized());
  }

  @Test
  void mensajesEnRutaExplicitaConMockMvcDevuelve200() throws Exception {
    when(servicioChat.listarMensajes(7L, 52L)).thenReturn(List.of());

    mockMvc
      .perform(get("/api/chat/conversaciones/52/mensajes").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isArray());
  }

  @Test
  void mensajesConIdInvalidoConMockMvcDevuelve400() throws Exception {
    mockMvc
      .perform(get("/api/chat/conversaciones/no-valido").session(session))
      .andExpect(status().isBadRequest());
  }

  @Test
  void mensajesInexistentesDevuelve404() throws Exception {
    when(servicioChat.listarMensajes(7L, 999L))
      .thenThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc
      .perform(get("/api/chat/conversaciones/999/mensajes").session(session))
      .andExpect(status().isNotFound());
  }

  @Test
  void mensajesSinPermisoDevuelve403() throws Exception {
    when(servicioChat.listarMensajes(7L, 52L))
      .thenThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN));

    mockMvc
      .perform(get("/api/chat/conversaciones/52/mensajes").session(session))
      .andExpect(status().isForbidden());
  }

  @Test
  void enviarMensajeConMockMvcDevuelve200() throws Exception {
    Mensaje mensaje = mensaje(16L, 7L, "Nuevo mensaje");
    when(servicioChat.enviar(7L, 52L, "Nuevo mensaje")).thenReturn(mensaje);

    mockMvc
      .perform(
        post("/api/chat/conversaciones/52/mensajes")
          .session(session)
          .param("contenido", "Nuevo mensaje")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(16))
      .andExpect(jsonPath("$.contenido").value("Nuevo mensaje"));
  }

  @Test
  void enviarMensajeSinContenidoConMockMvcDevuelve400() throws Exception {
    mockMvc
      .perform(post("/api/chat/conversaciones/52/mensajes").session(session))
      .andExpect(status().isBadRequest());
  }

  @Test
  void enviarMensajeVacioConMockMvcDevuelve400() throws Exception {
    mockMvc
      .perform(
        post("/api/chat/conversaciones/52/mensajes").session(session).param("contenido", "  ")
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void enviarMensajeInexistenteDevuelve404() throws Exception {
    doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND))
      .when(servicioChat)
      .enviar(7L, 999L, "Hola");

    mockMvc
      .perform(
        post("/api/chat/conversaciones/999/mensajes").session(session).param("contenido", "Hola")
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void enviarMensajeSinSesionConMockMvcDevuelve401() throws Exception {
    mockMvc
      .perform(post("/api/chat/conversaciones/52/mensajes").param("contenido", "Hola"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void enviarMensajeSinPermisoConMockMvcDevuelve403() throws Exception {
    doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN))
      .when(servicioChat)
      .enviar(7L, 52L, "Hola");

    mockMvc
      .perform(
        post("/api/chat/conversaciones/52/mensajes").session(session).param("contenido", "Hola")
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void marcarLeidoConMockMvcDevuelve200() throws Exception {
    when(servicioChat.contarNoLeidos(7L)).thenReturn(0L);

    mockMvc
      .perform(post("/api/chat/conversaciones/52/leido").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.cantidad").value(0));
  }

  @Test
  void marcarLeidoSinSesionConMockMvcDevuelve401() throws Exception {
    mockMvc.perform(post("/api/chat/conversaciones/52/leido")).andExpect(status().isUnauthorized());
  }

  @Test
  void marcarLeidoSinPermisoDevuelve403() throws Exception {
    doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN))
      .when(servicioChat)
      .marcarLeidos(7L, 52L);

    mockMvc
      .perform(post("/api/chat/conversaciones/52/leido").session(session))
      .andExpect(status().isForbidden());
  }

  @Test
  void marcarLeidoInexistenteDevuelve404() throws Exception {
    doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND))
      .when(servicioChat)
      .marcarLeidos(7L, 999L);

    mockMvc
      .perform(post("/api/chat/conversaciones/999/leido").session(session))
      .andExpect(status().isNotFound());
  }

  @Test
  void listarConversacionesFormateaLaHoraComoString() {
    HttpSession session = request.getSession();
    session.setAttribute("USUARIO_ID", 7L);
    Postulacion postulacion = new Postulacion();
    postulacion.setId(10L);
    postulacion.setTitulo("Desarrollador");
    postulacion.setFechaPostulacion(LocalDateTime.of(2026, 11, 4, 18, 30, 15));
    when(servicioChat.listarConversaciones(7L)).thenReturn(List.of(postulacion));
    when(servicioChat.listarMensajes(7L, 10L)).thenReturn(new ArrayList<>());

    ResponseEntity<?> respuesta = controlador.conversaciones(request);

    assertThat(respuesta.getStatusCode(), equalTo(HttpStatus.OK));
    Map<?, ?> conversacion = (Map<?, ?>) ((List<?>) respuesta.getBody()).get(0);
    assertThat(conversacion.get("hora"), equalTo("2026-11-04T18:30:15"));
  }

  @Test
  void enviarEnConversacionAjenaPropaga403() {
    HttpSession session = request.getSession();
    session.setAttribute("USUARIO_ID", 7L);
    doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN))
      .when(servicioChat)
      .enviar(7L, 10L, "hola");

    org.springframework.web.server.ResponseStatusException excepcion =
      org.junit.jupiter.api.Assertions.assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () -> controlador.enviar(10L, "hola", request)
      );

    assertThat(excepcion.getStatusCode(), equalTo(HttpStatus.FORBIDDEN));
  }

  private Mensaje mensaje(Long id, Long remitenteId, String contenido) {
    Usuario remitente = new Usuario();
    remitente.setId(remitenteId);
    Mensaje mensaje = new Mensaje();
    mensaje.setId(id);
    mensaje.setRemitente(remitente);
    mensaje.setContenido(contenido);
    mensaje.setFechaEnvio(LocalDateTime.of(2026, 11, 4, 18, 30));
    return mensaje;
  }
}
