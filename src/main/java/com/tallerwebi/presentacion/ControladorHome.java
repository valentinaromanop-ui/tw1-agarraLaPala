package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import java.util.HashMap;
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

  @Autowired
  public ControladorHome(ServicioOfertaEmpleador servicioOfertaEmpleador) {
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
  }

  @RequestMapping(path = "/home", method = RequestMethod.GET)
  public ModelAndView mostrarHome() {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("ofertasEmpleadores", servicioOfertaEmpleador.listarOfertasPublicadas());
    return new ModelAndView("home", modelo);
  }
}
