package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioConexionGoogle;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorGoogleCalendar {

  private static final String ESTADO = "GOOGLE_CALENDAR_STATE";
  private static final String PERFIL = "redirect:/empleador/perfil";
  private static final String ERROR = "googleError";
  private final ServicioConexionGoogle servicioConexionGoogle;

  @Autowired
  public ControladorGoogleCalendar(ServicioConexionGoogle servicioConexionGoogle) {
    this.servicioConexionGoogle = servicioConexionGoogle;
  }

  @RequestMapping(path = "/google-calendar/conectar", method = RequestMethod.POST)
  public ModelAndView conectar(HttpServletRequest request, RedirectAttributes atributos) {
    HttpSession session = request.getSession(false);
    Long empleadorId = getEmpleadorId(session);
    byte[] aleatorio = new byte[32];
    new SecureRandom().nextBytes(aleatorio);
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
    try {
      String url = servicioConexionGoogle.iniciar(empleadorId, state);
      synchronized (session) {
        session.setAttribute(
          ESTADO,
          new EstadoOAuth(state, empleadorId, Instant.now().plusSeconds(600))
        );
      }
      return new ModelAndView("redirect:" + url);
    } catch (IllegalStateException | ConexionGoogleException excepcion) {
      atributos.addFlashAttribute(
        ERROR,
        "No se pudo iniciar la conexión. Revisá la configuración de Google Calendar."
      );
      return new ModelAndView(PERFIL);
    }
  }

  @RequestMapping(path = "/google-calendar/callback", method = RequestMethod.GET)
  public ModelAndView callback(
    HttpServletRequest request,
    @RequestParam(name = "state", required = false) String state,
    @RequestParam(name = "code", required = false) String code,
    @RequestParam(name = "error", required = false) String error,
    RedirectAttributes atributos
  ) {
    HttpSession session = request.getSession(false);
    Long empleadorId = getEmpleadorId(session);
    if (!consumirEstado(session, state, empleadorId)) {
      atributos.addFlashAttribute(
        ERROR,
        "La autorización venció o no es válida. Volvé a conectar Google Calendar."
      );
      return new ModelAndView(PERFIL);
    }
    if (error != null || code == null || code.isBlank()) {
      atributos.addFlashAttribute(ERROR, "No se autorizó la conexión con Google Calendar.");
      return new ModelAndView(PERFIL);
    }
    try {
      servicioConexionGoogle.conectar(empleadorId, code);
      atributos.addFlashAttribute("googleMensaje", "Google Calendar conectado.");
    } catch (IllegalStateException | ConexionGoogleException excepcion) {
      atributos.addFlashAttribute(
        ERROR,
        "No se pudo completar la conexión con Google Calendar. Volvé a intentar."
      );
    }
    return new ModelAndView(PERFIL);
  }

  private boolean consumirEstado(HttpSession session, String state, Long empleadorId) {
    synchronized (session) {
      Object guardado = session.getAttribute(ESTADO);
      session.removeAttribute(ESTADO);
      return (
        guardado instanceof EstadoOAuth esperado &&
        esperado.valor().equals(state) &&
        esperado.empleadorId().equals(empleadorId) &&
        Instant.now().isBefore(esperado.vence())
      );
    }
  }

  private Long getEmpleadorId(HttpSession session) {
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number id)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    if (!"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return id.longValue();
  }

  record EstadoOAuth(String valor, Long empleadorId, Instant vence) {}
}
