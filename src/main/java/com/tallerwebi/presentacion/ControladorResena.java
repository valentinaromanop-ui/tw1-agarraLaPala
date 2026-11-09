package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Resena;
import com.tallerwebi.dominio.ResumenResenas;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorResena {

  private static final String VISTA_EMPRESA = "empresa";
  private static final String MENSAJE = "mensaje";
  private static final String ERROR = "error";
  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String PARAMETRO_EMPLEADOR_ID = "empleadorId";
  private static final String PARAMETRO_RESENA_ID = "resenaId";

  private final ServicioResena servicioResena;

  @Autowired
  public ControladorResena(ServicioResena servicioResena) {
    this.servicioResena = servicioResena;
  }

  @GetMapping("/empresas/{empleadorId}")
  public ModelAndView mostrarPerfilEmpresa(
    @PathVariable(PARAMETRO_EMPLEADOR_ID) Long empleadorId,
    HttpSession session
  ) {
    Usuario empleador = servicioResena.obtenerEmpleador(empleadorId);
    if (empleador == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    Long usuarioId = obtenerUsuarioId(session);
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("empleador", empleador);
    modelo.put("resenas", servicioResena.listarPorEmpleador(empleadorId));
    modelo.put("resumenResenas", servicioResena.obtenerResumen(empleadorId));
    modelo.put(
      "resenaPropia",
      usuarioId == null ? null : servicioResena.buscarPorCandidatoYEmpleador(usuarioId, empleadorId)
    );
    modelo.put(
      "puedeResenar",
      usuarioId != null && servicioResena.puedeResenar(usuarioId, empleadorId)
    );
    return new ModelAndView(VISTA_EMPRESA, modelo);
  }

  @PostMapping("/empresas/{empleadorId}/resenas")
  public String crearResena(
    @PathVariable(PARAMETRO_EMPLEADOR_ID) Long empleadorId,
    @RequestParam("puntaje") int puntaje,
    @RequestParam("comentario") String comentario,
    HttpSession session,
    RedirectAttributes redirect
  ) {
    Long candidatoId = obtenerUsuarioId(session);
    if (candidatoId == null) {
      return REDIRECT_LOGIN;
    }
    try {
      servicioResena.crear(candidatoId, empleadorId, puntaje, comentario);
      redirect.addFlashAttribute(MENSAJE, "Tu reseña fue publicada.");
    } catch (IllegalArgumentException excepcion) {
      redirect.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return redirigirEmpresa(empleadorId);
  }

  @PostMapping("/empresas/{empleadorId}/resenas/{resenaId}/editar")
  public String editarResena(
    @PathVariable(PARAMETRO_EMPLEADOR_ID) Long empleadorId,
    @PathVariable(PARAMETRO_RESENA_ID) Long resenaId,
    @RequestParam("puntaje") int puntaje,
    @RequestParam("comentario") String comentario,
    HttpSession session,
    RedirectAttributes redirect
  ) {
    Long candidatoId = obtenerUsuarioId(session);
    if (candidatoId == null) {
      return REDIRECT_LOGIN;
    }
    try {
      servicioResena.editar(candidatoId, empleadorId, resenaId, puntaje, comentario);
      redirect.addFlashAttribute(MENSAJE, "Tu reseña fue actualizada.");
    } catch (IllegalArgumentException excepcion) {
      redirect.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return redirigirEmpresa(empleadorId);
  }

  @PostMapping("/empresas/{empleadorId}/resenas/{resenaId}/eliminar")
  public String eliminarResena(
    @PathVariable(PARAMETRO_EMPLEADOR_ID) Long empleadorId,
    @PathVariable(PARAMETRO_RESENA_ID) Long resenaId,
    HttpSession session,
    RedirectAttributes redirect
  ) {
    Long candidatoId = obtenerUsuarioId(session);
    if (candidatoId == null) {
      return REDIRECT_LOGIN;
    }
    try {
      servicioResena.eliminar(candidatoId, empleadorId, resenaId);
      redirect.addFlashAttribute(MENSAJE, "Tu reseña fue eliminada.");
    } catch (IllegalArgumentException excepcion) {
      redirect.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return redirigirEmpresa(empleadorId);
  }

  @PostMapping("/empresas/{empleadorId}/resenas/{resenaId}/responder")
  public String responderResena(
    @PathVariable(PARAMETRO_EMPLEADOR_ID) Long empleadorId,
    @PathVariable(PARAMETRO_RESENA_ID) Long resenaId,
    @RequestParam("respuesta") String respuesta,
    HttpSession session,
    RedirectAttributes redirect
  ) {
    Long usuarioId = obtenerUsuarioId(session);
    if (usuarioId == null) {
      return REDIRECT_LOGIN;
    }
    try {
      servicioResena.responder(usuarioId, resenaId, respuesta);
      redirect.addFlashAttribute(MENSAJE, "Tu respuesta fue publicada.");
    } catch (IllegalArgumentException excepcion) {
      redirect.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return redirigirEmpresa(empleadorId);
  }

  @PostMapping("/empresas/{empleadorId}/resenas/{resenaId}/reportar")
  public String reportarResena(
    @PathVariable(PARAMETRO_EMPLEADOR_ID) Long empleadorId,
    @PathVariable(PARAMETRO_RESENA_ID) Long resenaId,
    HttpSession session,
    RedirectAttributes redirect
  ) {
    try {
      servicioResena.reportar(empleadorId, resenaId, obtenerUsuarioId(session));
      redirect.addFlashAttribute(MENSAJE, "El reporte fue guardado.");
    } catch (IllegalArgumentException excepcion) {
      redirect.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return redirigirEmpresa(empleadorId);
  }

  private String redirigirEmpresa(Long empleadorId) {
    return "redirect:/empresas/" + empleadorId;
  }

  private Long obtenerUsuarioId(HttpSession session) {
    if (session == null) {
      return null;
    }
    Object usuarioId = session.getAttribute("USUARIO_ID");
    return usuarioId instanceof Number ? ((Number) usuarioId).longValue() : null;
  }
}
