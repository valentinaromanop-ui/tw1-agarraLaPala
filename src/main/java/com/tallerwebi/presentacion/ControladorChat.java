package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorChat {

  private final ServicioMensaje servicioMensaje;

  @Autowired
  public ControladorChat(ServicioMensaje servicioMensaje) {
    this.servicioMensaje = servicioMensaje;
  }

  @RequestMapping(path = "/chat/{postulacionId}", method = RequestMethod.GET)
  public ModelAndView verChat(
    @PathVariable("postulacionId") Long postulacionId,
    HttpServletRequest request
  ) {
    Object id = request.getSession().getAttribute("USUARIO_ID");
    if (!(id instanceof Number)) {
      return new ModelAndView("redirect:/login");
    }
    Long usuarioId = ((Number) id).longValue();

    Map<String, Object> modelo = new HashMap<>();
    try {
      modelo.put("mensajes", servicioMensaje.listar(postulacionId, usuarioId));
    } catch (AccesoChatDenegado e) {
      return new ModelAndView("redirect:/home");
    }
    modelo.put("postulacionId", postulacionId);
    modelo.put("usuarioId", usuarioId);
    return new ModelAndView("chat", modelo);
  }
}
