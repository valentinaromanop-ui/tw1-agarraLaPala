package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.ServicioVacante;
import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorVacante {

  private ServicioVacante servicioVacante;
  private static final String VISTA_VACANTES = "vacantes";

  @Autowired
  public ControladorVacante(ServicioVacante servicioVacante) {
    this.servicioVacante = servicioVacante;
  }

  @RequestMapping(path = "/vacantes/recomendadas", method = RequestMethod.GET)
  public ModelAndView mostrarVacantesRecomendadas() {
    // Datos de prueba hasta integrar los datos del perfil del usuario.
    Map<String, Object> modelo = new ModelMap();
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();
    busqueda.setSkills(Arrays.asList("Java", "SQL", "PHP"));
    modelo.put("skills", busqueda.getSkills());
    try {
      modelo.put("vacantes", servicioVacante.obtenerVacantesPorSkills(busqueda));
    } catch (BusquedaVacanteInvalida | FuenteVacanteNoDisponible excepcion) {
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    return new ModelAndView(VISTA_VACANTES, modelo);
  }
}
