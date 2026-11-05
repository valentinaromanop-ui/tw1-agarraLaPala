package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ArchivoCvPostulanteDTO;
import com.tallerwebi.dominio.CupoPostulacionesCompletoException;
import com.tallerwebi.dominio.EstadoPostulacion;
import com.tallerwebi.dominio.FotoPerfilPostulanteDTO;
import com.tallerwebi.dominio.OfertaCerradaException;
import com.tallerwebi.dominio.PerfilPostulanteDTO;
import com.tallerwebi.dominio.PostulanteOfertaDTO;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteDTO;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ControladorPostulacionTest {

  private final ServicioPostulacion servicio = mock(ServicioPostulacion.class);
  private final ServicioOfertaEmpleador servicioOferta = mock(ServicioOfertaEmpleador.class);
  private final ServicioPago servicioPago = mock(ServicioPago.class);
  private final ControladorPostulacion controlador = new ControladorPostulacion(
    servicio,
    servicioOferta,
    servicioPago
  );

  @Test
  void laRutaDePerfilEsSoloGet() throws NoSuchMethodException {
    Method metodo =
      ControladorPostulacion.class.getMethod(
          "mostrarPerfilPostulante",
          Long.class,
          Long.class,
          HttpSession.class
        );

    assertTrue(metodo.isAnnotationPresent(GetMapping.class));
    assertEquals(
      "/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil",
      metodo.getAnnotation(GetMapping.class).value()[0]
    );
  }

  @Test
  void rechazaCandidatosAntesDeConsultarElPerfil() {
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute("ROL", "CANDIDATO");
    sesion.setAttribute("USUARIO_ID", 15L);

    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> controlador.mostrarPerfilPostulante(10L, 20L, sesion)
    );

    assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode());
    verify(servicio, never()).obtenerPerfilPostulante(10L, 20L, 15L);
  }

  @Test
  void laVistaRecibeSoloElDtoDeLectura() {
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute("ROL", "EMPLEADOR");
    sesion.setAttribute("USUARIO_ID", 42L);
    PerfilPostulanteDTO perfil = new PerfilPostulanteDTO(
      "Ada Lovelace",
      "ada@example.com",
      "AL",
      false,
      false,
      java.util.List.of(),
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of(),
      java.util.Map.of(),
      "Desarrolladora Java",
      java.time.LocalDateTime.of(2026, 10, 20, 15, 30),
      com.tallerwebi.dominio.EstadoPostulacion.PENDIENTE
    );
    org.mockito.Mockito.when(servicio.obtenerPerfilPostulante(10L, 20L, 42L)).thenReturn(perfil);

    org.springframework.web.servlet.ModelAndView resultado = controlador.mostrarPerfilPostulante(
      10L,
      20L,
      sesion
    );

    assertEquals("perfil-postulante-lectura", resultado.getViewName());
    assertEquals(perfil, resultado.getModel().get("perfilPostulante"));
    assertEquals(10L, resultado.getModel().get("ofertaId"));
    assertEquals(20L, resultado.getModel().get("postulacionId"));
  }

  @Test
  void postularseSinRolDeCandidatoDevuelve401SinCrearPostulacion() {
    ResponseEntity<Map<String, Object>> respuesta = controlador.postularse("LOCAL:10", request());

    assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
    assertEquals("Iniciá sesión como postulante.", respuesta.getBody().get("mensaje"));
    verify(servicio, never()).crearPostulacion(any(), any());
  }

  @Test
  void postularseConVacanteLocalRegistraClickYDevuelveConfirmacion() {
    Vacante vacante = new Vacante();
    vacante.setEmpleadorId(88L);
    when(servicio.obtenerVacante("LOCAL", 10L)).thenReturn(vacante);
    MockHttpServletRequest request = request("CANDIDATO", 15L);

    ResponseEntity<Map<String, Object>> respuesta = controlador.postularse("LOCAL:10", request);

    assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    assertEquals("Postulación registrada", respuesta.getBody().get("mensaje"));
    verify(servicioOferta).registrarClickPostular(10L);
    verify(servicio).crearPostulacion(15L, "LOCAL:10");
  }

  @Test
  void postularseMapeaCupoAgotadoYOfertaCerrada() {
    MockHttpServletRequest request = request("POSTULANTE", 15L);
    org.mockito.Mockito
      .doThrow(new CupoPostulacionesCompletoException())
      .when(servicio)
      .crearPostulacion(15L, "JOBICY:10");

    ResponseEntity<Map<String, Object>> cupo = controlador.postularse("JOBICY:10", request);
    assertEquals(HttpStatus.CONFLICT, cupo.getStatusCode());
    assertEquals(false, cupo.getBody().get("ok"));

    org.mockito.Mockito
      .doThrow(new OfertaCerradaException())
      .when(servicio)
      .crearPostulacion(15L, "LOCAL:11");
    ResponseEntity<Map<String, Object>> cerrada = controlador.postularse("LOCAL:11", request);
    assertEquals(HttpStatus.BAD_REQUEST, cerrada.getStatusCode());
  }

  @Test
  void registrarValidaSesionYFuenteDeLaOferta() {
    ResponseEntity<Map<String, Object>> sinSesion = controlador.registrar(
      "JOBICY:10",
      "Puesto",
      "Empresa",
      "Remoto",
      "remoto",
      "https://example.com",
      request()
    );
    ResponseEntity<Map<String, Object>> fuenteInvalida = controlador.registrar(
      "OTRA:10",
      "Puesto",
      "Empresa",
      "Remoto",
      "remoto",
      "https://example.com",
      request("CANDIDATO", 15L)
    );

    assertEquals(HttpStatus.UNAUTHORIZED, sinSesion.getStatusCode());
    assertEquals(HttpStatus.BAD_REQUEST, fuenteInvalida.getStatusCode());
    verify(servicio, never()).registrarEnVacante(any(), any(VacanteDTO.class));
  }

  @Test
  void obtenerIdsExigePostulanteYDevuelveSusOfertas() {
    assertEquals(HttpStatus.UNAUTHORIZED, controlador.obtenerIds(request()).getStatusCode());
    when(servicio.obtenerOfertasPostuladas(15L)).thenReturn(List.of("JOBICY:10"));

    var respuesta = controlador.obtenerIds(request("CANDIDATO", 15L));

    assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    assertEquals(List.of("JOBICY:10"), respuesta.getBody());
  }

  @Test
  void mostrarPostulantesAplicaFiltrosPremiumYExponeMetricas() {
    PostulanteOfertaDTO java = postulante(
      1L,
      "Java",
      EstadoPostulacion.ACEPTADA,
      LocalDateTime.of(2026, 11, 3, 10, 0)
    );
    PostulanteOfertaDTO sql = postulante(
      2L,
      "SQL",
      EstadoPostulacion.PENDIENTE,
      LocalDateTime.of(2026, 11, 4, 10, 0)
    );
    PostulanteOfertaDTO javaPremium = postulante(
      3L,
      "Java",
      EstadoPostulacion.ACEPTADA,
      LocalDateTime.of(2026, 11, 2, 10, 0),
      true
    );
    Vacante vacante = new Vacante();
    vacante.setId(10L);
    when(servicioPago.esPremiumActivo(42L)).thenReturn(true);
    when(servicio.obtenerPostulantes(10L, 42L)).thenReturn(List.of(java, sql, javaPremium));
    when(servicio.obtenerVacante("LOCAL", 10L)).thenReturn(vacante);
    when(servicioOferta.obtenerVistas(10L)).thenReturn(30L);
    when(servicioOferta.obtenerClicksPostular(10L)).thenReturn(7L);
    when(servicioOferta.obtenerPostulaciones(10L)).thenReturn(3L);
    MockHttpSession session = session("EMPLEADOR", 42L);

    var resultado = controlador.mostrarPostulantes(
      10L,
      "ACEPTADA",
      "java",
      "2026-11-01",
      "2026-11-05",
      true,
      session
    );

    assertEquals("postulantes-oferta", resultado.getViewName());
    assertEquals(List.of(javaPremium), resultado.getModel().get("postulantes"));
    assertEquals(30L, resultado.getModel().get("vistas"));
    assertEquals(7L, resultado.getModel().get("clicksPostular"));
    assertEquals(3L, resultado.getModel().get("cantidadPostulacionesOferta"));
  }

  @Test
  void fechaInvalidaEnFiltroPremiumSeExponeComoErrorSinDescartarResultados() {
    PostulanteOfertaDTO candidato = postulante(
      1L,
      "Java",
      EstadoPostulacion.PENDIENTE,
      LocalDateTime.now()
    );
    when(servicioPago.esPremiumActivo(42L)).thenReturn(true);
    when(servicio.obtenerPostulantes(10L, 42L)).thenReturn(List.of(candidato));
    when(servicio.obtenerVacante("LOCAL", 10L)).thenReturn(new Vacante());

    var resultado = controlador.mostrarPostulantes(
      10L,
      "INVALIDO",
      null,
      "fecha-mal-formada",
      null,
      false,
      session("EMPLEADOR", 42L)
    );

    assertEquals(
      "Ingresá las fechas con un formato válido.",
      resultado.getModel().get("errorFiltro")
    );
    assertEquals(List.of(candidato), resultado.getModel().get("postulantes"));
  }

  @Test
  void empleadorPuedeDecidirYRecibeConfirmacionFlash() {
    RedirectAttributesModelMap mensajes = new RedirectAttributesModelMap();

    var resultado = controlador.aceptarPostulacion(10L, 20L, session("EMPLEADOR", 42L), mensajes);

    assertEquals("redirect:/ofertas/10/postulantes", resultado.getViewName());
    assertEquals("Postulante aceptado", mensajes.getFlashAttributes().get("mensaje"));
    verify(servicio).cambiarEstado(10L, 20L, 42L, EstadoPostulacion.ACEPTADA);
  }

  @Test
  void cvInexistenteEs404YCvDisponibleIncluyeTipoYDisposicion() {
    HttpSession session = session("EMPLEADOR", 42L);
    when(servicio.obtenerCvPostulante(10L, 20L, 42L)).thenReturn(null);
    assertEquals(
      HttpStatus.NOT_FOUND,
      controlador.verCvPostulante(10L, 20L, false, session).getStatusCode()
    );
    when(servicio.obtenerCvPostulante(10L, 20L, 42L))
      .thenReturn(new ArchivoCvPostulanteDTO("perfil.PDF", new byte[] { 1, 2 }));

    var respuesta = controlador.verCvPostulante(10L, 20L, true, session);

    assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    assertEquals("application/pdf", respuesta.getHeaders().getFirst("Content-Type"));
    assertEquals(
      true,
      respuesta.getHeaders().getFirst("Content-Disposition").startsWith("attachment")
    );
    assertEquals(2, respuesta.getBody().length);
  }

  @Test
  void fotoConTipoNoPermitidoNoSeEntrega() {
    when(servicio.obtenerFotoPerfilPostulante(10L, 20L, 42L))
      .thenReturn(new FotoPerfilPostulanteDTO("image/svg+xml", new byte[] { 1 }));

    var respuesta = controlador.verFotoPerfilPostulante(10L, 20L, session("EMPLEADOR", 42L));

    assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
  }

  @Test
  void listadoPostulacionesRedirigeSinSesionYAgregaEstadoPremium() {
    assertEquals("redirect:/login", controlador.mostrarPostulaciones(request()).getViewName());
    when(servicioPago.esPremiumActivo(15L)).thenReturn(true);
    when(servicio.listarPostulaciones(15L)).thenReturn(List.of());

    var resultado = controlador.mostrarPostulaciones(request("CANDIDATO", 15L));

    assertEquals("postulaciones", resultado.getViewName());
    assertEquals(true, resultado.getModel().get("esPremium"));
    assertEquals(List.of(), resultado.getModel().get("postulaciones"));
  }

  private MockHttpServletRequest request() {
    return new MockHttpServletRequest();
  }

  private MockHttpServletRequest request(String rol, Long usuarioId) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session(rol, usuarioId));
    return request;
  }

  private MockHttpSession session(String rol, Long usuarioId) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", rol);
    session.setAttribute("USUARIO_ID", usuarioId);
    return session;
  }

  private PostulanteOfertaDTO postulante(
    Long id,
    String habilidad,
    EstadoPostulacion estado,
    LocalDateTime fecha
  ) {
    return postulante(id, habilidad, estado, fecha, false);
  }

  private PostulanteOfertaDTO postulante(
    Long id,
    String habilidad,
    EstadoPostulacion estado,
    LocalDateTime fecha,
    boolean premium
  ) {
    return new PostulanteOfertaDTO(
      id,
      "Candidato " + id,
      "candidato" + id + "@example.com",
      List.of(habilidad),
      fecha,
      false,
      premium,
      estado
    );
  }
}
