package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteSkill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorOfertaEmpleador {

  private static final String RUTA_CREAR = "/crear-postulacion";
  private static final String RUTA_BUSQUEDAS = "/busquedas-Empleador";
  private static final String VISTA_CREAR = "crear-postulacion";
  private static final String VISTA_BUSQUEDAS = "busquedas-Empleador";
  private static final String ATRIBUTO_DATOS = "datosOferta";
  private static final String PUBLICADA_TRUE = Boolean.TRUE.toString();
  private static final List<String> MODALIDADES = List.of("presencial", "remoto", "híbrido");

  private final ServicioOfertaEmpleador servicioOfertaEmpleador;

  @Autowired
  public ControladorOfertaEmpleador(ServicioOfertaEmpleador servicioOfertaEmpleador) {
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
  }

  @RequestMapping(path = RUTA_CREAR, method = RequestMethod.GET)
  public ModelAndView mostrarFormulario(HttpServletRequest request) {
    Usuario empleador = obtenerEmpleador(request);
    if (empleador == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put(ATRIBUTO_DATOS, new DatosOfertaEmpleador());
    modelo.put("empresa", empleador.getEmpresa());
    return new ModelAndView(VISTA_CREAR, modelo);
  }

  @RequestMapping(path = RUTA_CREAR, method = RequestMethod.POST)
  public ModelAndView crearOferta(
    @ModelAttribute(ATRIBUTO_DATOS) DatosOfertaEmpleador datos,
    HttpServletRequest request
  ) {
    Usuario empleador = obtenerEmpleador(request);
    if (empleador == null) {
      return new ModelAndView("redirect:/login");
    }

    List<String> errores = new ArrayList<>();
    validar(datos, errores);
    if (!errores.isEmpty()) {
      return mostrarFormularioConErrores(datos, empleador, errores);
    }

    Vacante vacante = crearVacante(datos, empleador);
    servicioOfertaEmpleador.publicar(vacante);
    return new ModelAndView("redirect:" + RUTA_BUSQUEDAS + "?publicada=true");
  }

  @RequestMapping(path = RUTA_BUSQUEDAS, method = RequestMethod.GET)
  public ModelAndView mostrarBusquedas(HttpServletRequest request) {
    Usuario empleador = obtenerEmpleador(request);
    if (empleador == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("vacantes", servicioOfertaEmpleador.listarPorEmpleador(empleador.getId()));
    if (PUBLICADA_TRUE.equals(request.getParameter("publicada"))) {
      modelo.put("mensaje", "La postulación se publicó correctamente.");
    }
    return new ModelAndView(VISTA_BUSQUEDAS, modelo);
  }

  private ModelAndView mostrarFormularioConErrores(
    DatosOfertaEmpleador datos,
    Usuario empleador,
    List<String> errores
  ) {
    Map<String, Object> modelo = new ModelMap();
    modelo.put(ATRIBUTO_DATOS, datos);
    modelo.put("empresa", empleador.getEmpresa());
    modelo.put("errores", errores);
    return new ModelAndView(VISTA_CREAR, modelo);
  }

  private void validar(DatosOfertaEmpleador datos, List<String> errores) {
    validarCamposObligatorios(datos, errores);
    validarModalidad(datos.getModalidad(), errores);
    validarSalario(datos.getSalario(), errores);
    validarSkills(datos.getSkills(), errores);
  }

  private void validarCamposObligatorios(DatosOfertaEmpleador datos, List<String> errores) {
    if (vacio(datos.getTitulo())) {
      errores.add("Ingresá el título del cargo.");
    }
    if (vacio(datos.getDescripcion())) {
      errores.add("Ingresá la descripción de la oferta.");
    }
    if (vacio(datos.getUbicacion())) {
      errores.add("Ingresá la ubicación o zona.");
    }
    if (vacio(datos.getHorario())) {
      errores.add("Ingresá el horario de trabajo.");
    }
  }

  private void validarModalidad(String modalidad, List<String> errores) {
    if (vacio(modalidad) || !MODALIDADES.contains(modalidad.trim().toLowerCase(Locale.ROOT))) {
      errores.add("Elegí una modalidad válida: presencial, remoto o híbrido.");
    }
  }

  private void validarSalario(String salario, List<String> errores) {
    if (vacio(salario)) {
      errores.add("Ingresá el salario ofrecido.");
    } else {
      try {
        if (new BigDecimal(salario.trim()).compareTo(BigDecimal.ZERO) <= 0) {
          errores.add("El salario debe ser mayor que cero.");
        }
      } catch (NumberFormatException excepcion) {
        errores.add("Ingresá un salario numérico válido.");
      }
    }
  }

  private void validarSkills(String skills, List<String> errores) {
    if (normalizarSkills(skills).isEmpty()) {
      errores.add("Ingresá al menos una skill requerida, separada por comas.");
    }
  }

  private Vacante crearVacante(DatosOfertaEmpleador datos, Usuario empleador) {
    Vacante vacante = new Vacante();
    vacante.setTitulo(datos.getTitulo().trim());
    vacante.setEmpresa(empleador.getEmpresa());
    vacante.setDescripcion(datos.getDescripcion().trim());
    vacante.setUbicacion(datos.getUbicacion().trim());
    vacante.setModalidad(datos.getModalidad().trim().toLowerCase(Locale.ROOT));
    vacante.setHorario(datos.getHorario().trim());
    vacante.setSalario(new BigDecimal(datos.getSalario().trim()));
    vacante.setEmpleadorId(empleador.getId());
    vacante.setFechaPublicacion(LocalDateTime.now());

    Set<VacanteSkill> relaciones = new HashSet<>();
    for (String nombre : normalizarSkills(datos.getSkills())) {
      Skill skill = new Skill();
      skill.setNombre(nombre);
      VacanteSkill relacion = new VacanteSkill();
      relacion.setVacante(vacante);
      relacion.setSkill(skill);
      relaciones.add(relacion);
    }
    vacante.setVacanteSkills(relaciones);
    return vacante;
  }

  private List<String> normalizarSkills(String skills) {
    Map<String, String> unicas = new LinkedHashMap<>();
    if (!vacio(skills)) {
      for (String skill : skills.split(",")) {
        String nombre = skill.trim();
        if (!nombre.isEmpty()) {
          unicas.putIfAbsent(nombre.toLowerCase(Locale.ROOT), nombre);
        }
      }
    }
    return new ArrayList<>(unicas.values());
  }

  private boolean vacio(String valor) {
    return valor == null || valor.trim().isEmpty();
  }

  private Usuario obtenerEmpleador(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (
      session == null || !"EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))
    ) {
      return null;
    }
    Object usuarioId = session.getAttribute("USUARIO_ID");
    if (!(usuarioId instanceof Number)) {
      return null;
    }
    return servicioOfertaEmpleador.obtenerEmpleador(((Number) usuarioId).longValue());
  }
}
