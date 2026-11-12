package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.CalculadorCoincidenciaCv;
import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.ServicioCV;
import com.tallerwebi.dominio.ServicioCVImpl;
import com.tallerwebi.dominio.ServicioCurriculumGenerado;
import com.tallerwebi.dominio.ServicioEmpleoGuardado;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPerfil;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.ServicioVacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import com.tallerwebi.dominio.excepcion.FuenteVacanteNoDisponible;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorVacante {

  private ServicioVacante servicioVacante;
  private final ServicioPostulacion servicioPostulacion;
  private final ServicioPerfil servicioPerfil;
  private final ServicioPago servicioPago;
  private final ServicioEmpleoGuardado servicioEmpleoGuardado;
  private final ServicioCurriculumGenerado servicioCurriculum;
  private final ServicioCV servicioCV;
  private ServicioResena servicioResena;
  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private static final String ROL_CANDIDATO = "CANDIDATO";
  private static final String ROL_POSTULANTE = "POSTULANTE";
  private static final String RUTA_LOGIN = "redirect:/login";
  private static final String BUSQUEDA = "busqueda";
  private static final String VISTA_VACANTES = "vacantes";
  private static final String ERROR_SESION_POSTULANTE = "Iniciá sesión como postulante.";

  @Autowired
  public ControladorVacante(
    ServicioVacante servicioVacante,
    ServicioPostulacion servicioPostulacion,
    ServicioPerfil servicioPerfil,
    ServicioPago servicioPago,
    ServicioEmpleoGuardado servicioEmpleoGuardado,
    ServicioCurriculumGenerado servicioCurriculum,
    ServicioCV servicioCV
  ) {
    this.servicioVacante = servicioVacante;
    this.servicioPostulacion = servicioPostulacion;
    this.servicioPerfil = servicioPerfil;
    this.servicioPago = servicioPago;
    this.servicioEmpleoGuardado = servicioEmpleoGuardado;
    this.servicioCurriculum = servicioCurriculum;
    this.servicioCV = servicioCV == null ? new ServicioCVImpl() : servicioCV;
  }

  @Autowired(required = false)
  public void setServicioResena(ServicioResena servicioResena) {
    this.servicioResena = servicioResena;
  }

  public ControladorVacante(
    ServicioVacante servicioVacante,
    ServicioPostulacion servicioPostulacion,
    ServicioPerfil servicioPerfil
  ) {
    this(servicioVacante, servicioPostulacion, servicioPerfil, null, null, null, null);
  }

  public ControladorVacante(
    ServicioVacante servicioVacante,
    ServicioPostulacion servicioPostulacion,
    ServicioPerfil servicioPerfil,
    ServicioPago servicioPago,
    ServicioEmpleoGuardado servicioEmpleoGuardado
  ) {
    this(
      servicioVacante,
      servicioPostulacion,
      servicioPerfil,
      servicioPago,
      servicioEmpleoGuardado,
      null,
      null
    );
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

    return mostrarResultados(busqueda, null, false, session);
  }

  @RequestMapping(path = "/vacantes/filtrar", method = RequestMethod.GET)
  public ModelAndView filtrarVacantes(
    @ModelAttribute(BUSQUEDA) BusquedaVacanteDTO busqueda,
    BindingResult errores,
    @RequestParam(name = "usarCv", defaultValue = "false") boolean usarCv,
    HttpSession session
  ) {
    if (
      errores.hasErrors() ||
      (busqueda.getSueldoMinimo() != null && busqueda.getSueldoMinimo().signum() < 0)
    ) {
      return mostrarResultados(
        busqueda,
        "Ingresá un sueldo mínimo válido, mayor o igual a cero.",
        usarCv,
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
        usarCv,
        session
      );
    }
    return mostrarResultados(busqueda, null, usarCv, session);
  }

  public ModelAndView filtrarVacantes(
    BusquedaVacanteDTO busqueda,
    BindingResult errores,
    HttpSession session
  ) {
    return filtrarVacantes(busqueda, errores, false, session);
  }

  @SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.AvoidDuplicateLiterals" })
  private ModelAndView mostrarResultados(
    BusquedaVacanteDTO busqueda,
    String error,
    boolean usarCv,
    HttpSession session
  ) {
    Object usuarioId = session.getAttribute("USUARIO_ID");
    if (!(usuarioId instanceof Number)) {
      return new ModelAndView(RUTA_LOGIN);
    }
    Object rol = session.getAttribute("ROL");
    if (ROL_EMPLEADOR.equalsIgnoreCase(String.valueOf(rol))) {
      return new ModelAndView("redirect:/home-empleador");
    }
    if (!esPostulante(rol)) {
      return new ModelAndView(RUTA_LOGIN);
    }
    Long idPostulante = ((Number) usuarioId).longValue();
    Map<String, Object> modelo = new ModelMap();
    busqueda.setSkills(servicioPerfil.obtenerHabilidades(idPostulante));
    modelo.put("skills", busqueda.getSkills());
    modelo.put(BUSQUEDA, busqueda);
    modelo.put("esPremium", servicioPago != null && servicioPago.esPremiumActivo(idPostulante));
    modelo.put("tieneCv", tieneCv(idPostulante));
    modelo.put("usarCv", usarCv);
    modelo.put(
      "premiumHasta",
      servicioPago == null ? null : servicioPago.fechaFinPremium(idPostulante)
    );
    ModelAndView redireccionGuardados = cargarEmpleosGuardados(modelo, idPostulante);
    if (redireccionGuardados != null) {
      return redireccionGuardados;
    }
    if (busqueda.getSkills().isEmpty()) {
      session.removeAttribute(VISTA_VACANTES);
      session.removeAttribute(BUSQUEDA);
      modelo.put("error", "Completá las habilidades de tu perfil para ver vacantes recomendadas.");
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    if (usarCv) {
      if (!Boolean.TRUE.equals(modelo.get("esPremium"))) {
        modelo.put("error", "El filtro por CV está disponible para candidatos Premium.");
        return new ModelAndView(VISTA_VACANTES, modelo);
      }
      CurriculumGenerado curriculum = obtenerCurriculum(idPostulante);
      if (curriculum == null || textoCv(curriculum).isBlank()) {
        modelo.put("error", "Subí o seleccioná un CV antes de usar este filtro.");
        return new ModelAndView(VISTA_VACANTES, modelo);
      }
      modelo.put("cvTipo", curriculum.getContenidoAts() != null ? "ATS" : "seleccionado");
      modelo.put(
        "cvNombre",
        curriculum.getContenidoAts() != null ? "CV ATS" : curriculum.getNombreArchivoOriginal()
      );
    }
    if (error != null) {
      modelo.put("error", error);
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    try {
      List<VacanteDTO> vacantes = servicioVacante.obtenerVacantesPorSkills(busqueda);
      if (usarCv) {
        vacantes = aplicarFiltroCv(vacantes, textoCv(obtenerCurriculum(idPostulante)), modelo);
      }
      EnriquecedorResenasVacantes.agregarResumenes(vacantes, servicioResena);
      modelo.put(VISTA_VACANTES, vacantes);
      session.setAttribute(VISTA_VACANTES, modelo.get(VISTA_VACANTES));
      session.setAttribute(BUSQUEDA, busqueda);
    } catch (BusquedaVacanteInvalida | FuenteVacanteNoDisponible excepcion) {
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView(VISTA_VACANTES, modelo);
    }
    return new ModelAndView(VISTA_VACANTES, modelo);
  }

  private List<VacanteDTO> aplicarFiltroCv(
    List<VacanteDTO> vacantes,
    String textoCv,
    Map<String, Object> modelo
  ) {
    List<VacanteDTO> filtrables = new ArrayList<>(vacantes);
    List<String> disponibles = new ArrayList<>();
    for (VacanteDTO vacante : filtrables) {
      if (vacante.getSkills() != null) {
        disponibles.addAll(vacante.getSkills());
      }
    }
    List<String> detectadas = servicioCV.extraerHabilidades(textoCv, disponibles);
    modelo.put("cvSkills", detectadas);
    if (detectadas.isEmpty()) {
      modelo.put("cvSinSkills", true);
      return vacantes;
    }
    return filtrables
      .stream()
      .filter(vacante ->
        vacante.getSkills() != null &&
        !servicioCV.extraerHabilidades(textoCv, vacante.getSkills()).isEmpty()
      )
      .toList();
  }

  private CurriculumGenerado obtenerCurriculum(Long usuarioId) {
    return servicioCurriculum == null ? null : servicioCurriculum.obtener(usuarioId);
  }

  private boolean tieneCv(Long usuarioId) {
    CurriculumGenerado curriculum = obtenerCurriculum(usuarioId);
    return curriculum != null && !textoCv(curriculum).isBlank();
  }

  private String textoCv(CurriculumGenerado curriculum) {
    String texto = curriculum.getContenidoAts();
    return texto == null || texto.isBlank() ? curriculum.getTextoOriginal() : texto;
  }

  private boolean esPostulante(Object rol) {
    return (
      ROL_CANDIDATO.equalsIgnoreCase(String.valueOf(rol)) ||
      ROL_POSTULANTE.equalsIgnoreCase(String.valueOf(rol))
    );
  }

  private ModelAndView cargarEmpleosGuardados(Map<String, Object> modelo, Long usuarioId) {
    try {
      modelo.put(
        "guardadas",
        servicioEmpleoGuardado == null
          ? java.util.List.of()
          : servicioEmpleoGuardado.listarIds(usuarioId)
      );
      return null;
    } catch (IllegalArgumentException excepcion) {
      if (ERROR_SESION_POSTULANTE.equals(excepcion.getMessage())) {
        return new ModelAndView("redirect:/home");
      }
      throw excepcion;
    }
  }
}
