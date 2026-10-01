package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.VacanteDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorBusquedaVacantes {

  private final CatalogoVacantesPrueba catalogo;

  public ControladorBusquedaVacantes(CatalogoVacantesPrueba catalogo) {
    this.catalogo = catalogo;
  }

  @RequestMapping(path = "/buscar", method = RequestMethod.GET)
  public ModelAndView mostrarBusqueda(
    @RequestParam(name = "texto", required = false, defaultValue = "") String texto
  ) {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("texto", texto);
    return new ModelAndView("busqueda", modelo);
  }

  @RequestMapping(path = "/vacantes/sugerencias", method = RequestMethod.GET)
  @ResponseBody
  public List<String> sugerencias(
    @RequestParam(name = "q", required = false, defaultValue = "") String consulta
  ) {
    return catalogo.obtenerTitulos(consulta);
  }

  @RequestMapping(path = "/vacantes", method = RequestMethod.GET)
  @ResponseBody
  public List<VacanteResultadoBusqueda> buscarVacantes(
    @RequestParam(name = "texto", required = false, defaultValue = "") String texto,
    @RequestParam(name = "zona", required = false, defaultValue = "") String zona,
    @RequestParam(name = "modalidad", required = false, defaultValue = "") String modalidad,
    @RequestParam(name = "skills", required = false, defaultValue = "") String skills,
    @RequestParam(name = "fecha", required = false, defaultValue = "") String fecha
  ) {
    List<VacanteResultadoBusqueda> resultados = new ArrayList<>();
    for (VacanteDTO vacante : catalogo.buscar(texto, zona, modalidad, skills, fecha)) {
      resultados.add(new VacanteResultadoBusqueda(vacante));
    }
    return resultados;
  }
}
