package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.ServicioVacante;
import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
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
  private final ServicioPostulacion servicioPostulacion;
  private static final String VISTA_VACANTES = "vacantes";

  @Autowired
  public ControladorVacante(
    ServicioVacante servicioVacante,
    ServicioPostulacion servicioPostulacion
  ) {
    this.servicioVacante = servicioVacante;
    this.servicioPostulacion = servicioPostulacion;
  }

  @ModelAttribute("postuladas")
  public List<String> postuladas(HttpSession session) {
    return servicioPostulacion.obtenerOfertasPostuladas((Long) session.getAttribute("USUARIO_ID"));
  }

  @RequestMapping(path = "/vacantes/recomendadas", method = RequestMethod.GET)
  public ModelAndView mostrarVacantesRecomendadas(HttpSession session) {
    // Datos de prueba hasta integrar los datos del perfil del usuario.
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();

    return mostrarResultados(busqueda, null, session);
  }

  @RequestMapping(path = "/vacantes/filtrar", method = RequestMethod.GET)
  public ModelAndView filtrarVacantes(
    @ModelAttribute("busqueda") BusquedaVacanteDTO busqueda,
    BindingResult errores,
    HttpSession session
  ) {
    if (
      errores.hasErrors() ||
      (busqueda.getSueldoMinimo() != null && busqueda.getSueldoMinimo().signum() < 0)
    ) {
      return mostrarResultados(
        busqueda,
        "Ingresá un sueldo mínimo válido, mayor o igual a cero.",
        session
      );
    }
    if (
      busqueda.getSueldoMinimo() != null &&
      (busqueda.getMoneda() == null || busqueda.getMoneda().isBlank())
    ) {
      return mostrarResultados(
        busqueda,
        "Seleccioná una moneda para filtrar por sueldo mensual.",
        session
      );
    }
    return mostrarResultados(busqueda, null, session);
  }

  private ModelAndView mostrarResultados(
    BusquedaVacanteDTO busqueda,
    String error,
    HttpSession session
  ) {
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
      modelo.put(VISTA_VACANTES, servicioVacante.obtenerVacantesPorSkills(busqueda));
      session.setAttribute(VISTA_VACANTES, modelo.get(VISTA_VACANTES));
      session.setAttribute("busqueda", busqueda);
    } catch (BusquedaVacanteInvalida | FuenteVacanteNoDisponible excepcion) {
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    return new ModelAndView(VISTA_VACANTES, modelo);
  }
}
