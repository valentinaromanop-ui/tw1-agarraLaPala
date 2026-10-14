package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorHome {

  private final ServicioOfertaEmpleador servicioOfertaEmpleador;
  private final ServicioPago servicioPago;

  @Autowired
  public ControladorHome(
    ServicioOfertaEmpleador servicioOfertaEmpleador,
    ServicioPago servicioPago
  ) {
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
    this.servicioPago = servicioPago;
  }

  @RequestMapping(path = "/home", method = RequestMethod.GET)
  public ModelAndView mostrarHome(HttpServletRequest request) {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("ofertasEmpleadores", servicioOfertaEmpleador.listarOfertasPublicadas());
    Object usuarioId = request.getSession(false) == null
      ? null
      : request.getSession(false).getAttribute("USUARIO_ID");
    if (usuarioId instanceof Number) {
      modelo.put("premiumHasta", servicioPago.fechaFinPremium(((Number) usuarioId).longValue()));
    }
    return new ModelAndView("home", modelo);
  }
}
