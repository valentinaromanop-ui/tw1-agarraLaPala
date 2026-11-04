package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.NotificacionMensaje;
import com.tallerwebi.dominio.ServicioMensaje;
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

  private final ServicioPago servicioPago;
  private final ServicioMensaje servicioMensaje;

  public AtributosNavbarAdvice(ServicioPago servicioPago, ServicioMensaje servicioMensaje) {
    this.servicioPago = servicioPago;
    this.servicioMensaje = servicioMensaje;
  }

  @ModelAttribute("rutaInicio")
  public String rutaInicio(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return (
        session != null && "EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))
      )
      ? "/home-empleador"
      : "/home";
  }

  @ModelAttribute("premiumHasta")
  public LocalDateTime premiumHasta(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (
      session == null ||
      (!esPostulante(session.getAttribute("ROL")) &&
        !"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL"))))
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
}
