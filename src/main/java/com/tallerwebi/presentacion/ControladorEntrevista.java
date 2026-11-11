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
import org.springframework.ui.ModelMap;
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
  private static final String OFERTA_ID = "ofertaId";
  private static final String POSTULACION_ID = "postulacionId";
  private static final String MENSAJE = "mensaje";
  private static final String TURNOS_POR_DIA = "turnosPorDia";
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
    @PathVariable(OFERTA_ID) Long ofertaId,
    @PathVariable(POSTULACION_ID) Long postulacionId,
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
        MENSAJE,
        "Invitación registrada. Se solicitó el envío del correo al postulante."
      );
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return new ModelAndView("redirect:/ofertas/" + ofertaId + "/postulantes");
  }

  @RequestMapping(path = "/entrevistas/{postulacionId}", method = RequestMethod.GET)
  public ModelAndView mostrarInvitacion(
    @PathVariable(POSTULACION_ID) Long postulacionId,
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

    Map<String, Object> modelo = new ModelMap();
    modelo.put("entrevista", entrevista);
    modelo.put("zonaHoraria", ServicioEntrevista.ZONA_HORARIA);
    modelo.put(TURNOS_POR_DIA, Map.of());
    try {
      if (entrevista.getEstado() == EstadoEntrevista.INVITADA) {
        modelo.put(
          TURNOS_POR_DIA,
          servicioEntrevista.obtenerTurnosParaInvitacion(postulacionId, candidatoId)
        );
      } else if (entrevista.getEstado() == EstadoEntrevista.AGENDADA) {
        modelo.put(
          "entrevista",
          servicioEntrevista.actualizarEnlaceMeet(postulacionId, candidatoId)
        );
      }
    } catch (ConexionGoogleException excepcion) {
      modelo.put(ERROR, "No se pudo consultar Google Calendar. Volvé a intentar.");
    } catch (IllegalArgumentException excepcion) {
      modelo.put(ERROR, excepcion.getMessage());
    }
    return new ModelAndView("invitacion-entrevista", modelo);
  }

  @RequestMapping(path = "/entrevistas/{postulacionId}/confirmar", method = RequestMethod.POST)
  public ModelAndView confirmar(
    @PathVariable(POSTULACION_ID) Long postulacionId,
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
      mensajes.addFlashAttribute(MENSAJE, "Entrevista agendada.");
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

  @RequestMapping(
    path = "/ofertas/{ofertaId}/postulantes/{postulacionId}/entrevista",
    method = RequestMethod.GET
  )
  public ModelAndView gestionar(
    @PathVariable(OFERTA_ID) Long ofertaId,
    @PathVariable(POSTULACION_ID) Long postulacionId,
    HttpSession session
  ) {
    Long empleadorId = validarEmpleador(session);
    Map<String, Object> modelo = new ModelMap();
    modelo.put(OFERTA_ID, ofertaId);
    modelo.put(POSTULACION_ID, postulacionId);
    modelo.put("zonaHoraria", ServicioEntrevista.ZONA_HORARIA);
    modelo.put(TURNOS_POR_DIA, Map.of());
    try {
      Entrevista entrevista = servicioEntrevista.obtenerParaEmpleador(
        ofertaId,
        postulacionId,
        empleadorId
      );
      modelo.put("entrevista", entrevista);
      if (entrevista.getEstado() == EstadoEntrevista.AGENDADA) {
        modelo.put(
          TURNOS_POR_DIA,
          servicioEntrevista.obtenerTurnosParaEmpleador(ofertaId, postulacionId, empleadorId)
        );
      }
    } catch (IllegalArgumentException excepcion) {
      modelo.put(ERROR, excepcion.getMessage());
    } catch (ConexionGoogleException excepcion) {
      modelo.put(ERROR, "No se pudo consultar Google Calendar. Volvé a intentar.");
    }
    return new ModelAndView("gestionar-entrevista", modelo);
  }

  @RequestMapping(
    path = "/ofertas/{ofertaId}/postulantes/{postulacionId}/entrevista/modificar",
    method = RequestMethod.POST
  )
  public ModelAndView modificar(
    @PathVariable(OFERTA_ID) Long ofertaId,
    @PathVariable(POSTULACION_ID) Long postulacionId,
    @RequestParam(name = "inicio", required = false) String inicio,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long empleadorId = validarEmpleador(session);
    try {
      if (inicio == null || inicio.isBlank()) {
        throw new IllegalArgumentException("Elegí un horario para modificar la entrevista.");
      }
      servicioEntrevista.modificar(ofertaId, postulacionId, empleadorId, Instant.parse(inicio));
      mensajes.addFlashAttribute(MENSAJE, "Horario de entrevista actualizado.");
    } catch (DateTimeParseException excepcion) {
      mensajes.addFlashAttribute(ERROR, "El horario seleccionado no es válido.");
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute(ERROR, excepcion.getMessage());
    } catch (ConexionGoogleException excepcion) {
      mensajes.addFlashAttribute(
        ERROR,
        "No se pudo modificar en Google Calendar. Volvé a intentar."
      );
    }
    return new ModelAndView(
      "redirect:/ofertas/" + ofertaId + "/postulantes/" + postulacionId + "/entrevista"
    );
  }

  @RequestMapping(
    path = "/ofertas/{ofertaId}/postulantes/{postulacionId}/entrevista/cancelar",
    method = RequestMethod.POST
  )
  public ModelAndView cancelar(
    @PathVariable(OFERTA_ID) Long ofertaId,
    @PathVariable(POSTULACION_ID) Long postulacionId,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long empleadorId = validarEmpleador(session);
    try {
      servicioEntrevista.cancelar(ofertaId, postulacionId, empleadorId);
      mensajes.addFlashAttribute(MENSAJE, "Entrevista cancelada.");
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute(ERROR, excepcion.getMessage());
    } catch (ConexionGoogleException excepcion) {
      mensajes.addFlashAttribute(
        ERROR,
        "No se pudo cancelar en Google Calendar. Volvé a intentar."
      );
    }
    return new ModelAndView(
      "redirect:/ofertas/" + ofertaId + "/postulantes/" + postulacionId + "/entrevista"
    );
  }

  private Long validarEmpleador(HttpSession session) {
    Long usuarioId = obtenerUsuarioId(session);
    if (!"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return usuarioId;
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
