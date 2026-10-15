package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.ServicioEmpleoGuardado;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPerfil;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.ServicioVacante;
import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import jakarta.servlet.http.HttpSession;
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
  private final ServicioPerfil servicioPerfil;
  private final ServicioPago servicioPago;
  private final ServicioEmpleoGuardado servicioEmpleoGuardado;
  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private static final String ROL_CANDIDATO = "CANDIDATO";
  private static final String ROL_POSTULANTE = "POSTULANTE";
  private static final String RUTA_LOGIN = "redirect:/login";
  private static final String RUTA_BUSQUEDAS_EMPLEADOR = "redirect:/busquedas-Empleador";
  private static final String BUSQUEDA = "busqueda";
  private static final String VISTA_VACANTES = "vacantes";

  @Autowired
  public ControladorVacante(
    ServicioVacante servicioVacante,
    ServicioPostulacion servicioPostulacion,
    ServicioPerfil servicioPerfil,
    ServicioPago servicioPago,
    ServicioEmpleoGuardado servicioEmpleoGuardado
  ) {
    this.servicioVacante = servicioVacante;
    this.servicioPostulacion = servicioPostulacion;
    this.servicioPerfil = servicioPerfil;
    this.servicioPago = servicioPago;
    this.servicioEmpleoGuardado = servicioEmpleoGuardado;
  }

  public ControladorVacante(
    ServicioVacante servicioVacante,
    ServicioPostulacion servicioPostulacion,
    ServicioPerfil servicioPerfil
  ) {
    this(servicioVacante, servicioPostulacion, servicioPerfil, null, null);
  }

  @ModelAttribute("postuladas")
  public List<String> postuladas(HttpSession session) {
    Object usuarioId = session.getAttribute("USUARIO_ID");
    if (!esPostulante(session.getAttribute("ROL")) || !(usuarioId instanceof Number)) {
      return java.util.List.of();
    }
    return servicioPostulacion.obtenerOfertasPostuladas(((Number) usuarioId).longValue());
  }

  @RequestMapping(path = "/vacantes/recomendadas", method = RequestMethod.GET)
  public ModelAndView mostrarVacantesRecomendadas(HttpSession session) {
    BusquedaVacanteDTO busqueda = new BusquedaVacanteDTO();

    return mostrarResultados(busqueda, null, session);
  }

  @RequestMapping(path = "/vacantes/filtrar", method = RequestMethod.GET)
  public ModelAndView filtrarVacantes(
    @ModelAttribute(BUSQUEDA) BusquedaVacanteDTO busqueda,
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
    Object usuarioId = session.getAttribute("USUARIO_ID");
    if (!(usuarioId instanceof Number)) {
      return new ModelAndView(RUTA_LOGIN);
    }
    Object rol = session.getAttribute("ROL");
    if (ROL_EMPLEADOR.equalsIgnoreCase(String.valueOf(rol))) {
      return new ModelAndView(RUTA_BUSQUEDAS_EMPLEADOR);
    }
    if (!esPostulante(rol)) {
      return new ModelAndView(RUTA_LOGIN);
    }
    Long idPostulante = ((Number) usuarioId).longValue();
    Map<String, Object> modelo = new ModelMap();
    busqueda.setSkills(servicioPerfil.obtenerHabilidades(idPostulante));
    modelo.put("skills", busqueda.getSkills());
    modelo.put(BUSQUEDA, busqueda);
    modelo.put(
      "premiumHasta",
      servicioPago == null ? null : servicioPago.fechaFinPremium(idPostulante)
    );
    modelo.put(
      "guardadas",
      servicioEmpleoGuardado == null
        ? java.util.List.of()
        : servicioEmpleoGuardado.listarIds(idPostulante)
    );
    if (busqueda.getSkills().isEmpty()) {
      session.removeAttribute(VISTA_VACANTES);
      session.removeAttribute(BUSQUEDA);
      modelo.put("error", "Completá las habilidades de tu perfil para ver vacantes recomendadas.");
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    if (error != null) {
      modelo.put("error", error);
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    try {
      modelo.put(VISTA_VACANTES, servicioVacante.obtenerVacantesPorSkills(busqueda));
      session.setAttribute(VISTA_VACANTES, modelo.get(VISTA_VACANTES));
      session.setAttribute(BUSQUEDA, busqueda);
    } catch (BusquedaVacanteInvalida | FuenteVacanteNoDisponible excepcion) {
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    return new ModelAndView(VISTA_VACANTES, modelo);
  }

  private boolean esPostulante(Object rol) {
    return (
      ROL_CANDIDATO.equalsIgnoreCase(String.valueOf(rol)) ||
      ROL_POSTULANTE.equalsIgnoreCase(String.valueOf(rol))
    );
  }
}
