package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioInvitacion;
import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.ServicioNotificacionOferta;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorNotificacionOferta {

  private static final String ATRIBUTO_ROL = "ROL";
  private static final String ATRIBUTO_USUARIO_ID = "USUARIO_ID";
  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private final ServicioNotificacionOferta servicioNotificacionOferta;
  private ServicioInvitacion servicioInvitacion;
  private ServicioMensaje servicioMensaje;

  public ControladorNotificacionOferta(ServicioNotificacionOferta servicioNotificacionOferta) {
    this.servicioNotificacionOferta = servicioNotificacionOferta;
  }

  @Autowired
  public void setServicioInvitacion(ServicioInvitacion servicioInvitacion) {
    this.servicioInvitacion = servicioInvitacion;
  }

  @Autowired
  public void setServicioMensaje(ServicioMensaje servicioMensaje) {
    this.servicioMensaje = servicioMensaje;
  }

  @GetMapping("/notificaciones")
  public ModelAndView listar(HttpSession session) {
    Long usuarioId = obtenerCandidatoId(session);
    if (usuarioId == null) {
      return new ModelAndView(sesionIniciada(session) ? "redirect:/home" : "redirect:/login");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("notificaciones", servicioNotificacionOferta.listar(usuarioId));
    modelo.put("invitaciones", servicioInvitacion.listarParaCandidato(usuarioId));
    return new ModelAndView("notificaciones", modelo);
  }

  @GetMapping("/notificaciones/contador")
  @ResponseBody
  public ResponseEntity<Map<String, Long>> contador(HttpSession session) {
    Long usuarioId = obtenerUsuarioId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    String rol = String.valueOf(session.getAttribute(ATRIBUTO_ROL));
    if (!esCandidato(rol) && !ROL_EMPLEADOR.equalsIgnoreCase(rol)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    long cantidad = servicioMensaje.listarNoLeidos(usuarioId).size();
    if (esCandidato(rol)) {
      cantidad += servicioNotificacionOferta.contarNoLeidas(usuarioId);
    }
    Map<String, Long> respuesta = new HashMap<>();
    respuesta.put("count", cantidad);
    return ResponseEntity.ok(respuesta);
  }

  @PostMapping("/notificaciones/marcar-leidas")
  @ResponseBody
  public ResponseEntity<Void> marcarTodasLeidas(HttpSession session) {
    Long usuarioId = obtenerUsuarioId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    String rol = String.valueOf(session.getAttribute(ATRIBUTO_ROL));
    if (!esCandidato(rol) && !ROL_EMPLEADOR.equalsIgnoreCase(rol)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    servicioMensaje.marcarTodosLeidos(usuarioId);
    if (esCandidato(rol)) {
      servicioNotificacionOferta.marcarTodasLeidas(usuarioId);
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping({ "/notificaciones/{id}/abrir", "/notificaciones/ofertas/{id}/abrir" })
  public ModelAndView abrir(@PathVariable("id") Long id, HttpSession session) {
    Long usuarioId = obtenerCandidatoId(session);
    if (usuarioId == null) {
      return new ModelAndView(sesionIniciada(session) ? "redirect:/home" : "redirect:/login");
    }
    String enlace = servicioNotificacionOferta.marcarLeidaYObtenerEnlace(id, usuarioId);
    return new ModelAndView(enlace == null ? "redirect:/home" : "redirect:" + enlace);
  }

  private Long obtenerCandidatoId(HttpSession session) {
    if (session == null || !esCandidato(String.valueOf(session.getAttribute(ATRIBUTO_ROL)))) {
      return null;
    }
    return obtenerUsuarioId(session);
  }

  private Long obtenerUsuarioId(HttpSession session) {
    if (session == null || session.getAttribute(ATRIBUTO_ROL) == null) {
      return null;
    }
    Object id = session.getAttribute(ATRIBUTO_USUARIO_ID);
    return id instanceof Number ? ((Number) id).longValue() : null;
  }

  private boolean esCandidato(String rol) {
    return ("CANDIDATO".equalsIgnoreCase(rol) || "POSTULANTE".equalsIgnoreCase(rol));
  }

  private boolean sesionIniciada(HttpSession session) {
    return session != null && session.getAttribute(ATRIBUTO_USUARIO_ID) instanceof Number;
  }
}
