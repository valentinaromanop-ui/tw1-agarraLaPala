package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.CalculadorCoincidenciaCv;
import com.tallerwebi.dominio.CoincidenciaCv;
import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.ServicioCurriculumGenerado;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.infraestructura.VacanteMapper;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorBusquedaVacantes {

  private static final String PARAMETRO_TEXTO = "texto";
  private static final int MINIMO_COINCIDENCIA_CV = 30;
  private final CatalogoVacantesPrueba catalogo;
  private final ServicioOfertaEmpleador servicioOfertaEmpleador;
  private final VacanteMapper vacanteMapper;
  private final ServicioPago servicioPago;
  private final ServicioCurriculumGenerado servicioCurriculum;
  private ServicioResena servicioResena;

  public ControladorBusquedaVacantes(CatalogoVacantesPrueba catalogo) {
    this(catalogo, null, new VacanteMapper(), null, null);
  }

  @Autowired
  public ControladorBusquedaVacantes(
    CatalogoVacantesPrueba catalogo,
    ServicioOfertaEmpleador servicioOfertaEmpleador,
    VacanteMapper vacanteMapper,
    ServicioPago servicioPago,
    ServicioCurriculumGenerado servicioCurriculum
  ) {
    this.catalogo = catalogo;
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
    this.vacanteMapper = vacanteMapper;
    this.servicioPago = servicioPago;
    this.servicioCurriculum = servicioCurriculum;
  }

  @Autowired(required = false)
  public void setServicioResena(ServicioResena servicioResena) {
    this.servicioResena = servicioResena;
  }

  public ModelAndView mostrarBusqueda(
    @RequestParam(name = PARAMETRO_TEXTO, required = false, defaultValue = "") String texto
  ) {
    return mostrarBusqueda(texto, null);
  }

  @RequestMapping(path = "/buscar", method = RequestMethod.GET)
  public ModelAndView mostrarBusqueda(
    @RequestParam(name = PARAMETRO_TEXTO, required = false, defaultValue = "") String texto,
    HttpSession session
  ) {
    Map<String, Object> modelo = new ModelMap();
    modelo.put(PARAMETRO_TEXTO, texto);
    Long usuarioId = obtenerUsuarioId(session);
    boolean candidato = esCandidato(session);
    modelo.put("esPremium", candidato && usuarioId != null && esPremium(usuarioId));
    modelo.put("tieneCv", candidato && usuarioId != null && tieneCv(usuarioId));
    return new ModelAndView("busqueda", modelo);
  }

  @RequestMapping(path = "/vacantes/sugerencias", method = RequestMethod.GET)
  @ResponseBody
  public List<String> sugerencias(
    @RequestParam(name = "q", required = false, defaultValue = "") String consulta
  ) {
    List<String> titulosPublicados = new ArrayList<>();
    if (servicioOfertaEmpleador != null) {
      for (Vacante vacante : servicioOfertaEmpleador.listarOfertasPublicadas()) {
        titulosPublicados.add(vacante.getTitulo());
      }
    }
    return catalogo.obtenerTitulos(consulta, titulosPublicados);
  }

  @RequestMapping(path = "/vacantes", method = RequestMethod.GET)
  @ResponseBody
  public List<VacanteResultadoBusqueda> buscarVacantes(
    @RequestParam(name = PARAMETRO_TEXTO, required = false, defaultValue = "") String texto,
    @RequestParam(name = "zona", required = false, defaultValue = "") String zona,
    @RequestParam(name = "modalidad", required = false, defaultValue = "") String modalidad,
    @RequestParam(name = "skills", required = false, defaultValue = "") String skills,
    @RequestParam(name = "fecha", required = false, defaultValue = "") String fecha,
    @RequestParam(name = "usarCv", defaultValue = "false") boolean usarCv,
    HttpSession session
  ) {
    String textoCv = validarFiltroCv(usarCv, session);
    List<VacanteResultadoBusqueda> resultados = new ArrayList<>();
    List<VacanteDTO> ofertasPublicadas = new ArrayList<>();
    if (servicioOfertaEmpleador != null) {
      for (Vacante vacante : servicioOfertaEmpleador.listarOfertasPublicadas()) {
        ofertasPublicadas.add(vacanteMapper.toDTO(vacante));
      }
    }
    List<VacanteDTO> vacantes = catalogo.buscar(
      texto,
      zona,
      modalidad,
      skills,
      fecha,
      ofertasPublicadas
    );
    if (usarCv) {
      vacantes = filtrarPorCv(vacantes, textoCv);
    }
    vacantes.sort(
      Comparator
        .comparing(VacanteDTO::isEmpresaPremium)
        .reversed()
        .thenComparing(
          VacanteDTO::getFechaPublicacion,
          Comparator.nullsLast(Comparator.reverseOrder())
        )
    );
    EnriquecedorResenasVacantes.agregarResumenes(vacantes, servicioResena);
    for (VacanteDTO vacante : vacantes) {
      resultados.add(new VacanteResultadoBusqueda(vacante));
    }
    return resultados;
  }

  private List<VacanteDTO> filtrarPorCv(List<VacanteDTO> vacantes, String textoCv) {
    List<VacanteDTO> filtradas = new ArrayList<>();
    for (VacanteDTO vacante : vacantes) {
      CoincidenciaCv coincidencia = CalculadorCoincidenciaCv.calcular(textoCv, vacante);
      if (coincidencia.getPorcentaje() >= MINIMO_COINCIDENCIA_CV) {
        vacante.setPorcentajeCoincidenciaCv(coincidencia.getPorcentaje());
        vacante.setPalabrasCoincidentesCv(coincidencia.getPalabrasCoincidentes());
        filtradas.add(vacante);
      }
    }
    filtradas.sort(Comparator.comparing(VacanteDTO::getPorcentajeCoincidenciaCv).reversed());
    return filtradas;
  }

  private String validarFiltroCv(boolean usarCv, HttpSession session) {
    if (!usarCv) {
      return "";
    }
    Long usuarioId = validarUsuarioPremium(session);
    return obtenerTextoCv(usuarioId);
  }

  private Long validarUsuarioPremium(HttpSession session) {
    Long usuarioId = obtenerUsuarioId(session);
    if (esCandidato(session) && usuarioId != null && esPremium(usuarioId)) {
      return usuarioId;
    }
    throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "El filtro por CV es una función exclusiva para candidatos Premium."
    );
  }

  private String obtenerTextoCv(Long usuarioId) {
    CurriculumGenerado curriculum = servicioCurriculum.obtener(usuarioId);
    if (curriculum == null) {
      throw errorCvNoDisponible();
    }
    String texto = curriculum.getContenidoAts();
    if (texto == null || texto.isBlank()) {
      texto = curriculum.getTextoOriginal();
    }
    if (texto == null || texto.isBlank()) {
      throw errorCvNoDisponible();
    }
    return texto;
  }

  private ResponseStatusException errorCvNoDisponible() {
    return new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Subí o seleccioná un CV antes de usar este filtro."
    );
  }

  private boolean tieneCv(Long usuarioId) {
    if (servicioCurriculum == null) {
      return false;
    }
    CurriculumGenerado curriculum = servicioCurriculum.obtener(usuarioId);
    return (
      curriculum != null &&
      ((curriculum.getContenidoAts() != null && !curriculum.getContenidoAts().isBlank()) ||
        (curriculum.getTextoOriginal() != null && !curriculum.getTextoOriginal().isBlank()))
    );
  }

  private boolean esPremium(Long usuarioId) {
    return servicioPago != null && servicioPago.esPremiumActivo(usuarioId);
  }

  private Long obtenerUsuarioId(HttpSession session) {
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number id)) {
      return null;
    }
    return id.longValue();
  }

  private boolean esCandidato(HttpSession session) {
    if (session == null) {
      return false;
    }
    String rol = String.valueOf(session.getAttribute("ROL"));
    return "CANDIDATO".equalsIgnoreCase(rol) || "POSTULANTE".equalsIgnoreCase(rol);
  }
}
