package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteDTO;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorDetalleVacante {

  private static final String BUSQUEDA = "busqueda";
  private static final String VACANTES = "vacantes";
  private static final String USUARIO_ID = "USUARIO_ID";
  private static final String ERROR = "error";
  private static final String VISTA_DETALLE = "detalle-vacante";

  private static final String VOLVER = "redirect:/vacantes/resultados";
  private final ServicioPostulacion servicioPostulacion;

  @Autowired
  public ControladorDetalleVacante(ServicioPostulacion servicioPostulacion) {
    this.servicioPostulacion = servicioPostulacion;
  }

  @RequestMapping(path = "/vacantes/detalle", method = RequestMethod.GET)
  public ModelAndView mostrarDetalle(
    @RequestParam("fuente") String fuente,
    @RequestParam("id") Long id,
    HttpSession session
  ) {
    Object oferta = buscarEnResultados(fuente, id, session);
    if (oferta == null) {
      oferta = servicioPostulacion.obtenerVacante(fuente, id);
    }
    Map<String, Object> modelo = new ModelMap();
    if (oferta == null) {
      modelo.put(ERROR, "La oferta ya no está en esta búsqueda. Volvé a buscar vacantes.");
      return new ModelAndView(VISTA_DETALLE, modelo);
    }
    DatosPostulacion datosPostulacion = new DatosPostulacion();
    datosPostulacion.setFuente(fuente);
    datosPostulacion.setId(id);
    modelo.put("datosPostulacion", datosPostulacion);
    modelo.put("vacante", oferta);
    modelo.put("fuente", fuente);
    modelo.put("puedePostularse", esPostulante(session));
    modelo.put(
      "yaPostulado",
      servicioPostulacion
        .obtenerOfertasPostuladas((Long) session.getAttribute(USUARIO_ID))
        .contains(fuente + ":" + id)
    );
    return new ModelAndView(VISTA_DETALLE, modelo);
  }

  @RequestMapping(path = "/vacantes/postular", method = RequestMethod.POST)
  public ModelAndView postular(
    @ModelAttribute("datosPostulacion") DatosPostulacion datosPostulacion,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    if (!esPostulante(session)) {
      return new ModelAndView("redirect:/login");
    }
    String fuente = datosPostulacion.getFuente();
    Long id = datosPostulacion.getId();
    if (fuente == null || id == null) {
      mensajes.addFlashAttribute(ERROR, "Seleccioná una oferta para postularte.");
      return new ModelAndView(VOLVER);
    }
    VacanteDTO oferta = buscarEnResultados(fuente, id, session);
    if (oferta == null) {
      oferta = recuperarOfertaGuardada(fuente, id);
    }
    if (oferta == null) {
      mensajes.addFlashAttribute(ERROR, "Volvé a buscar la oferta antes de postularte.");
      return new ModelAndView(VOLVER);
    }
    try {
      servicioPostulacion.registrarEnVacante((Long) session.getAttribute(USUARIO_ID), oferta);
      mensajes.addFlashAttribute(
        "mensaje",
        "Tu postulación quedó registrada para " + oferta.getTitulo() + "."
      );
    } catch (IllegalArgumentException excepcion) {
      mensajes.addFlashAttribute(ERROR, excepcion.getMessage());
    }
    return new ModelAndView(VOLVER);
  }

  @RequestMapping(path = "/vacantes/resultados", method = RequestMethod.GET)
  public ModelAndView volverAResultados(HttpSession session) {
    if (session.getAttribute(VACANTES) == null || session.getAttribute(BUSQUEDA) == null) {
      return new ModelAndView("redirect:/vacantes/recomendadas");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put(VACANTES, session.getAttribute(VACANTES));
    modelo.put(BUSQUEDA, session.getAttribute(BUSQUEDA));
    BusquedaVacanteDTO busquedaVacanteDTO = (BusquedaVacanteDTO) session.getAttribute(BUSQUEDA);
    modelo.put("skills", busquedaVacanteDTO.getSkills());
    modelo.put(
      "postuladas",
      servicioPostulacion.obtenerOfertasPostuladas((Long) session.getAttribute(USUARIO_ID))
    );
    return new ModelAndView(VACANTES, modelo);
  }

  private VacanteDTO buscarEnResultados(String fuente, Long id, HttpSession session) {
    Object resultados = session.getAttribute(VACANTES);
    if (resultados instanceof List<?> lista) {
      for (Object elemento : lista) {
        if (
          elemento instanceof VacanteDTO oferta &&
          id.equals(oferta.getId()) &&
          fuente.equals(oferta.getFuente())
        ) {
          return oferta;
        }
      }
    }
    return null;
  }

  private VacanteDTO recuperarOfertaGuardada(String fuente, Long id) {
    Vacante guardada = servicioPostulacion.obtenerVacante(fuente, id);
    if (guardada == null) {
      return null;
    }
    VacanteDTO oferta = new VacanteDTO();
    oferta.setId(id);
    oferta.setFuente(fuente);
    oferta.setTitulo(guardada.getTitulo());
    return oferta;
  }

  private boolean esPostulante(HttpSession session) {
    Object rol = session.getAttribute("ROL");
    return (
      session.getAttribute(USUARIO_ID) != null &&
      ("CANDIDATO".equals(rol) || "POSTULANTE".equals(rol))
    );
  }
}
