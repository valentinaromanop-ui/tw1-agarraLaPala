package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPerfilEmpleador {

  private final ServicioOfertaEmpleador servicioOfertaEmpleador;

  public ControladorPerfilEmpleador(ServicioOfertaEmpleador servicioOfertaEmpleador) {
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
  }

  @RequestMapping(path = "/empleador/perfil", method = RequestMethod.GET)
  public ModelAndView mostrarPerfil(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("USUARIO_ID") == null) {
      return new ModelAndView("redirect:/login");
    }
    if (!"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    Object usuarioId = session.getAttribute("USUARIO_ID");
    if (!(usuarioId instanceof Number)) {
      return new ModelAndView("redirect:/login");
    }
    Long empleadorId = ((Number) usuarioId).longValue();
    Usuario empleador = servicioOfertaEmpleador.obtenerEmpleador(empleadorId);
    if (empleador == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("empleador", empleador);
    modelo.put("resumen", servicioOfertaEmpleador.obtenerResumen(empleadorId));
    return new ModelAndView("perfil-empleador", modelo);
  }
}
