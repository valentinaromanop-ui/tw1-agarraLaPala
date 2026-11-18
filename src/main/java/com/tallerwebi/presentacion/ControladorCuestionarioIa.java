package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioCuestionario;
import com.tallerwebi.dominio.excepcion.CuestionarioNoDisponible;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorCuestionarioIa {

  private static final String USUARIO_ID = "USUARIO_ID";
  private static final String REDIRIGIR_LOGIN = "redirect:/login";
  private static final String REDIRIGIR_POSTULACIONES = "redirect:/postulaciones";

  private final ServicioCuestionario servicioCuestionario;

  @Autowired
  public ControladorCuestionarioIa(ServicioCuestionario servicioCuestionario) {
    this.servicioCuestionario = servicioCuestionario;
  }

  @GetMapping("/cuestionario-ia/{postulacionId}")
  public ModelAndView mostrar(
    @PathVariable("postulacionId") Long postulacionId,
    HttpSession session
  ) {
    Long usuarioId = (Long) session.getAttribute(USUARIO_ID);
    if (usuarioId == null) {
      return new ModelAndView(REDIRIGIR_LOGIN);
    }
    try {
      ModelAndView vista = new ModelAndView("Cuestionario-IA");
      vista.addObject("preguntas", servicioCuestionario.obtenerPreguntas(postulacionId, usuarioId));
      vista.addObject("postulacionId", postulacionId);
      return vista;
    } catch (CuestionarioNoDisponible excepcion) {
      return new ModelAndView(REDIRIGIR_POSTULACIONES);
    }
  }

  @PostMapping("/cuestionario-ia/{postulacionId}")
  public String enviar(
    @PathVariable("postulacionId") Long postulacionId,
    @RequestParam("respuestas") List<String> respuestas,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long usuarioId = (Long) session.getAttribute(USUARIO_ID);
    if (usuarioId == null) {
      return REDIRIGIR_LOGIN;
    }
    try {
      var resultado = servicioCuestionario.responder(postulacionId, usuarioId, respuestas);
      mensajes.addFlashAttribute("resultadoIa", resultado);
    } catch (CuestionarioNoDisponible excepcion) {
      mensajes.addFlashAttribute("error", excepcion.getMessage());
    }
    return REDIRIGIR_POSTULACIONES;
  }
}
