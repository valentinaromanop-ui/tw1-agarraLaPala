package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioPago;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AtributosNavbarAdvice {

  private final ServicioPago servicioPago;

  public AtributosNavbarAdvice(ServicioPago servicioPago) {
    this.servicioPago = servicioPago;
  }

  @ModelAttribute("premiumHasta")
  public LocalDateTime premiumHasta(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !esPostulante(session.getAttribute("ROL"))) {
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
}
