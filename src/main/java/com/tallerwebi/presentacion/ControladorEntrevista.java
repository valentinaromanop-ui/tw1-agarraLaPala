package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.ServicioEntrevista;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorEntrevista {

  private final ServicioEntrevista servicioEntrevista;

  @Autowired
  public ControladorEntrevista(ServicioEntrevista servicioEntrevista) {
    this.servicioEntrevista = servicioEntrevista;
  }

  @RequestMapping(
    path = "/ofertas/{ofertaId}/postulantes/{postulacionId}/entrevista",
    method = RequestMethod.POST
  )
  public ModelAndView invitar(
    @PathVariable("ofertaId") Long ofertaId,
    @PathVariable("postulacionId") Long postulacionId,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long empleadorId = obtenerUsuarioId(session);
    if (!"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    try {
      servicioEntrevista.invitar(ofertaId, postulacionId, empleadorId);
      mensajes.addFlashAttribute(
        "mensaje",
        "Invitación registrada. Se solicitó el envío del correo al postulante."
      );
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute("error", excepcion.getMessage());
    }
    return new ModelAndView("redirect:/ofertas/" + ofertaId + "/postulantes");
  }

  @RequestMapping(path = "/entrevistas/{postulacionId}", method = RequestMethod.GET)
  public ModelAndView mostrarInvitacion(
    @PathVariable("postulacionId") Long postulacionId,
    HttpServletRequest request
  ) {
    HttpSession session = request.getSession(false);
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number)) {
      return new ModelAndView("redirect:/login");
    }
    String rol = String.valueOf(session.getAttribute("ROL"));
    if (!"CANDIDATO".equalsIgnoreCase(rol) && !"POSTULANTE".equalsIgnoreCase(rol)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    try {
      Entrevista entrevista = servicioEntrevista.obtenerInvitacion(
        postulacionId,
        obtenerUsuarioId(session)
      );
      return new ModelAndView("invitacion-entrevista", "entrevista", entrevista);
    } catch (IllegalArgumentException excepcion) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, excepcion.getMessage(), excepcion);
    }
  }

  private Long obtenerUsuarioId(HttpSession session) {
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number id)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return id.longValue();
  }
}
