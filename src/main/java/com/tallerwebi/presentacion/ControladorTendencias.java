package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.HabilidadTop;
import com.tallerwebi.dominio.ServicioTendencias;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorTendencias {

  private static final int LIMITE = 10;
  private final ServicioTendencias servicioTendencias;

  @Autowired
  public ControladorTendencias(ServicioTendencias servicioTendencias) {
    this.servicioTendencias = servicioTendencias;
  }

  @GetMapping("/habilidades/top")
  public ModelAndView top() {
    ModelAndView vista = new ModelAndView("top-habilidades");
    vista.addObject("top", servicioTendencias.obtenerTop(LIMITE));
    return vista;
  }

  private static final int LIMITE_POPUP = 3;

  @GetMapping(value = "/habilidades/top/datos", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<HabilidadTop> topDatos() {
    return servicioTendencias.obtenerTop(LIMITE_POPUP);
  }
}
