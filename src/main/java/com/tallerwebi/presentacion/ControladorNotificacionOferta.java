package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioNotificacionOferta;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorNotificacionOferta {

  private final ServicioNotificacionOferta servicioNotificacionOferta;

  public ControladorNotificacionOferta(ServicioNotificacionOferta servicioNotificacionOferta) {
    this.servicioNotificacionOferta = servicioNotificacionOferta;
  }

  @GetMapping("/notificaciones/ofertas/{id}/abrir")
  public ModelAndView abrir(@PathVariable("id") Long id, HttpSession session) {
    Object usuarioId = session.getAttribute("USUARIO_ID");
    Object rol = session.getAttribute("ROL");
    if (!(usuarioId instanceof Number)) {
      return new ModelAndView("redirect:/login");
    }
    if (
      !"CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) &&
      !"POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    ) {
      return new ModelAndView("redirect:/home");
    }
    String enlace = servicioNotificacionOferta.marcarLeidaYObtenerEnlace(
      id,
      ((Number) usuarioId).longValue()
    );
    return new ModelAndView(enlace == null ? "redirect:/home" : "redirect:" + enlace);
  }
}
