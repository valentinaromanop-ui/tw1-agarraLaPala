package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NotificacionMensaje;
import com.tallerwebi.dominio.NotificacionOferta;
import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.ServicioNotificacionOferta;
import com.tallerwebi.dominio.ServicioPago;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AtributosNavbarAdvice {

  private static final String ATRIBUTO_ROL = "ROL";
  private final ServicioPago servicioPago;
  private final ServicioMensaje servicioMensaje;
  private final ServicioNotificacionOferta servicioNotificacionOferta;

  public AtributosNavbarAdvice(
    ServicioPago servicioPago,
    ServicioMensaje servicioMensaje,
    ServicioNotificacionOferta servicioNotificacionOferta
  ) {
    this.servicioPago = servicioPago;
    this.servicioMensaje = servicioMensaje;
    this.servicioNotificacionOferta = servicioNotificacionOferta;
  }

  @ModelAttribute("rutaInicio")
  public String rutaInicio(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return (
        session != null &&
        "EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute(ATRIBUTO_ROL)))
      )
      ? "/home-empleador"
      : "/home";
  }

  @ModelAttribute("premiumHasta")
  public LocalDateTime premiumHasta(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (
      session == null ||
      (!esPostulante(session.getAttribute(ATRIBUTO_ROL)) &&
        !"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute(ATRIBUTO_ROL))))
    ) {
      return null;
    }
    Object id = session.getAttribute("USUARIO_ID");
    if (!(id instanceof Number)) {
      return null;
    }
    return servicioPago.fechaFinPremium(((Number) id).longValue());
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }

  @ModelAttribute("mensajesNoLeidos")
  public List<NotificacionMensaje> mensajesNoLeidos(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    if (!(id instanceof Number)) {
      return new ArrayList<>();
    }
    return servicioMensaje.listarNoLeidos(((Number) id).longValue());
  }

  @ModelAttribute("alertasOfertasNoLeidas")
  public List<NotificacionOferta> alertasOfertasNoLeidas(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !esPostulante(session.getAttribute(ATRIBUTO_ROL))) {
      return List.of();
    }
    Object id = session.getAttribute("USUARIO_ID");
    if (!(id instanceof Number)) {
      return List.of();
    }
    return servicioNotificacionOferta.listarNoLeidas(((Number) id).longValue());
  }
}
