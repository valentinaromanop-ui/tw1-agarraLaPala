package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.ServicioPostulacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPostulacion {

  private static final String RUTA_POSTULACIONES = "/postulaciones";

  private final ServicioPostulacion servicioPostulacion;

  @Autowired
  public ControladorPostulacion(ServicioPostulacion servicioPostulacion) {
    this.servicioPostulacion = servicioPostulacion;
  }

  @RequestMapping(path = "/postularse", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity<Map<String, String>> postularse(
    @RequestParam("ofertaId") String ofertaId,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(mensaje("Iniciá sesión como postulante."));
    }
    servicioPostulacion.crearPostulacion(usuarioId, ofertaId);
    return ResponseEntity.ok(mensaje("Postulación enviada"));
  }

  @RequestMapping(path = RUTA_POSTULACIONES, method = RequestMethod.GET)
  public ModelAndView mostrarPostulaciones(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("postulaciones", servicioPostulacion.listarPostulaciones(usuarioId));
    return new ModelAndView("postulaciones", modelo);
  }

  private Map<String, String> mensaje(String texto) {
    Map<String, String> respuesta = new HashMap<>();
    respuesta.put("mensaje", texto);
    return respuesta;
  }

  private Long obtenerUsuarioPostulante(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object rol = session == null ? null : session.getAttribute("ROL");
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    if (!esPostulante(rol) || !(id instanceof Number)) {
      return null;
    }
    return ((Number) id).longValue();
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }
}
