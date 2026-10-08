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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();

    return mostrarResultados(busqueda, null);
  }

  @RequestMapping(path = "/vacantes/filtrar", method = RequestMethod.GET)
  public ModelAndView filtrarVacantes(
    @ModelAttribute("busqueda") BusquedaVacanteDTO busqueda,
    BindingResult errores
  ) {
    if (
      errores.hasErrors() ||
      (busqueda.getSueldoMinimo() != null && busqueda.getSueldoMinimo().signum() < 0)
    ) {
      return mostrarResultados(busqueda, "Ingresá un sueldo mínimo válido, mayor o igual a cero.");
    }
    if (
      busqueda.getSueldoMinimo() != null &&
      (busqueda.getMoneda() == null || busqueda.getMoneda().isBlank())
    ) {
      return mostrarResultados(busqueda, "Seleccioná una moneda para filtrar por sueldo mensual.");
    }
    return mostrarResultados(busqueda, null);
  }

  private ModelAndView mostrarResultados(BusquedaVacanteDTO busqueda, String error) {
    Map<String, Object> modelo = new ModelMap();
    // Las skills pertenecen al perfil, no a los filtros enviados por el formulario.
    busqueda.setSkills(Arrays.asList("Java", "SQL", "PHP"));
    modelo.put("skills", busqueda.getSkills());
    modelo.put("busqueda", busqueda);
    if (error != null) {
      modelo.put("error", error);
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    try {
      modelo.put("vacantes", servicioVacante.obtenerVacantesPorSkills(busqueda));
    } catch (BusquedaVacanteInvalida | FuenteVacanteNoDisponible excepcion) {
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    return new ModelAndView(VISTA_VACANTES, modelo);
  }
}
