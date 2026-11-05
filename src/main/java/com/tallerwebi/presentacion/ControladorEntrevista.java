package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.EstadoEntrevista;
import com.tallerwebi.dominio.ServicioEntrevista;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorEntrevista {

  private static final String ERROR = "error";
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
      mensajes.addFlashAttribute(ERROR, excepcion.getMessage());
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
    validarCandidato(session);
    Long candidatoId = obtenerUsuarioId(session);
    Entrevista entrevista;
    try {
      entrevista = servicioEntrevista.obtenerInvitacion(postulacionId, candidatoId);
    } catch (IllegalArgumentException excepcion) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, excepcion.getMessage(), excepcion);
    }

    ModelAndView vista = new ModelAndView("invitacion-entrevista", "entrevista", entrevista);
    vista.addObject("zonaHoraria", ServicioEntrevista.ZONA_HORARIA);
    vista.addObject("turnosPorDia", Map.of());
    try {
      if (entrevista.getEstado() == EstadoEntrevista.INVITADA) {
        vista.addObject(
          "turnosPorDia",
          servicioEntrevista.obtenerTurnosParaInvitacion(postulacionId, candidatoId)
        );
      } else if (entrevista.getUrlMeet() == null) {
        vista.addObject(
          "entrevista",
          servicioEntrevista.actualizarEnlaceMeet(postulacionId, candidatoId)
        );
      }
    } catch (ConexionGoogleException excepcion) {
      vista.addObject(ERROR, "No se pudo consultar Google Calendar. Volvé a intentar.");
    } catch (IllegalArgumentException excepcion) {
      vista.addObject(ERROR, excepcion.getMessage());
    }
    return vista;
  }

  @RequestMapping(path = "/entrevistas/{postulacionId}/confirmar", method = RequestMethod.POST)
  public ModelAndView confirmar(
    @PathVariable("postulacionId") Long postulacionId,
    @RequestParam(name = "inicio", required = false) String inicio,
    HttpServletRequest request,
    RedirectAttributes mensajes
  ) {
    HttpSession session = request.getSession(false);
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number)) {
      return new ModelAndView("redirect:/login");
    }
    validarCandidato(session);
    try {
      if (inicio == null || inicio.isBlank()) {
        throw new IllegalArgumentException("Elegí un horario para confirmar la entrevista.");
      }
      servicioEntrevista.confirmar(postulacionId, obtenerUsuarioId(session), Instant.parse(inicio));
      mensajes.addFlashAttribute("mensaje", "Entrevista agendada.");
    } catch (DateTimeParseException excepcion) {
      mensajes.addFlashAttribute(ERROR, "El horario seleccionado no es válido.");
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute(ERROR, excepcion.getMessage());
    } catch (ConexionGoogleException excepcion) {
      mensajes.addFlashAttribute(
        ERROR,
        "No se pudo confirmar con Google Calendar. Volvé a intentar."
      );
    }
    return new ModelAndView("redirect:/entrevistas/" + postulacionId);
  }

  private void validarCandidato(HttpSession session) {
    String rol = String.valueOf(session.getAttribute("ROL"));
    if (!"CANDIDATO".equalsIgnoreCase(rol) && !"POSTULANTE".equalsIgnoreCase(rol)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  private Long obtenerUsuarioId(HttpSession session) {
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number id)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return id.longValue();
  }
}
