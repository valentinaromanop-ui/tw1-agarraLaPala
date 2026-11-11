package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.CupoPostulacionesCompletoException;
import com.tallerwebi.dominio.OfertaCerradaException;
import com.tallerwebi.dominio.ServicioInvitacion;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorInvitacion {

  private final ServicioInvitacion servicioInvitacion;

  public ControladorInvitacion(ServicioInvitacion servicioInvitacion) {
    this.servicioInvitacion = servicioInvitacion;
  }

  @PostMapping("/ofertas/{ofertaId}/invitaciones")
  public ModelAndView invitar(
    @PathVariable("ofertaId") Long ofertaId,
    @RequestParam("candidatoId") Long candidatoId,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long empleadorId = obtenerId(session, "EMPLEADOR");
    if (empleadorId == null) {
      return new ModelAndView("redirect:/login");
    }
    try {
      servicioInvitacion.invitar(empleadorId, candidatoId, ofertaId);
      mensajes.addFlashAttribute("mensaje", "La invitación se envió correctamente.");
    } catch (OfertaCerradaException | CupoPostulacionesCompletoException excepcion) {
      mensajes.addFlashAttribute("error", excepcion.getMessage());
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute("error", excepcion.getMessage());
    }
    return new ModelAndView("redirect:/ofertas/" + ofertaId + "/postulantes");
  }

  @PostMapping("/invitaciones/{id}/rechazar")
  public ModelAndView rechazar(
    @PathVariable("id") Long invitacionId,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long candidatoId = obtenerId(session, "CANDIDATO");
    if (candidatoId == null) {
      candidatoId = obtenerId(session, "POSTULANTE");
    }
    if (candidatoId == null) {
      return new ModelAndView("redirect:/login");
    }
    try {
      servicioInvitacion.rechazar(candidatoId, invitacionId);
      mensajes.addFlashAttribute("mensaje", "La invitación fue rechazada.");
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute("error", excepcion.getMessage());
    }
    return new ModelAndView("redirect:/notificaciones");
  }

  private Long obtenerId(HttpSession session, String rolEsperado) {
    if (
      session == null || !rolEsperado.equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))
    ) {
      return null;
    }
    Object id = session.getAttribute("USUARIO_ID");
    return id instanceof Number ? ((Number) id).longValue() : null;
  }
}
