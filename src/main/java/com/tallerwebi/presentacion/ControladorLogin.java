package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorLogin {

  private static final String ATRIBUTO_ERROR = "error";
  private static final String RUTA_VACANTES_RECOMENDADAS = "redirect:/vacantes/recomendadas";
  private static final String RUTA_BUSQUEDAS_EMPLEADOR = "redirect:/busquedas-Empleador";
  private static final Pattern FORMATO_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final List<String> HABILIDADES_DISPONIBLES = List.of(
    "Java",
    "JavaScript",
    "SQL",
    "HTML/CSS",
    "Git",
    "Spring",
    "React",
    "PHP"
  );

  private final ServicioLogin servicioLogin;

  @Autowired
  public ControladorLogin(ServicioLogin servicioLogin) {
    this.servicioLogin = servicioLogin;
  }

  @RequestMapping("/login")
  public ModelAndView irALogin() {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosLogin", new DatosLogin());
    return new ModelAndView("login", modelo);
  }

  @RequestMapping(path = "/registro", method = RequestMethod.GET)
  public ModelAndView irARegistro() {
    return new ModelAndView("registro");
  }

  @RequestMapping(path = "/registro/empleador", method = RequestMethod.GET)
  public ModelAndView irARegistroEmpleador() {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosRegistroEmpleador", new DatosRegistroEmpleador());
    return new ModelAndView("registro-empleador", modelo);
  }

  @RequestMapping(path = "/registro/candidato", method = RequestMethod.GET)
  public ModelAndView irARegistroCandidato() {
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosRegistroCandidato", new DatosRegistroCandidato());
    modelo.put("habilidadesDisponibles", HABILIDADES_DISPONIBLES);
    return new ModelAndView("registro-candidato", modelo);
  }

  @RequestMapping(path = "/registro/empleador", method = RequestMethod.POST)
  public ModelAndView registrarEmpleador(
    @ModelAttribute("datosRegistroEmpleador") DatosRegistroEmpleador datosRegistroEmpleador
  ) {
    if (
      campoVacio(datosRegistroEmpleador.getEmpresa()) ||
      campoVacio(datosRegistroEmpleador.getLegajo()) ||
      campoVacio(datosRegistroEmpleador.getEmail()) ||
      campoVacio(datosRegistroEmpleador.getPassword()) ||
      campoVacio(datosRegistroEmpleador.getConfirmarPassword())
    ) {
      return mostrarErrorRegistroEmpleador(datosRegistroEmpleador, "Completá todos los campos.");
    }
    if (!correoValido(datosRegistroEmpleador.getEmail())) {
      return mostrarErrorRegistroEmpleador(
        datosRegistroEmpleador,
        "Ingresá un correo electrónico válido."
      );
    }
    if (
      !datosRegistroEmpleador.getPassword().equals(datosRegistroEmpleador.getConfirmarPassword())
    ) {
      return mostrarErrorRegistroEmpleador(datosRegistroEmpleador, "Las contraseñas no coinciden.");
    }
    Usuario usuario = new Usuario();
    usuario.setEmail(datosRegistroEmpleador.getEmail());
    usuario.setPassword(datosRegistroEmpleador.getPassword());
    usuario.setRol("EMPLEADOR");
    usuario.setEmpresa(datosRegistroEmpleador.getEmpresa().trim());
    usuario.setLegajo(datosRegistroEmpleador.getLegajo().trim());
    try {
      servicioLogin.registrar(usuario);
    } catch (UsuarioExistente e) {
      return mostrarErrorRegistroEmpleador(
        datosRegistroEmpleador,
        "Ya existe una cuenta registrada con ese correo."
      );
    }
    identificarUsuarioEnSesion(usuario);
    return new ModelAndView(RUTA_BUSQUEDAS_EMPLEADOR);
  }

  @RequestMapping(path = "/registro/candidato", method = RequestMethod.POST)
  public ModelAndView registrarCandidato(
    @ModelAttribute("datosRegistroCandidato") DatosRegistroCandidato datosRegistroCandidato
  ) {
    String errorValidacion = validarRegistroCandidato(datosRegistroCandidato);
    if (errorValidacion != null) {
      return mostrarErrorRegistroCandidato(datosRegistroCandidato, errorValidacion);
    }
    Usuario usuario = new Usuario();
    usuario.setNombre(datosRegistroCandidato.getNombre().trim());
    usuario.setApellido(datosRegistroCandidato.getApellido().trim());
    usuario.setEmail(datosRegistroCandidato.getEmail());
    usuario.setPassword(datosRegistroCandidato.getPassword());
    usuario.setRol("CANDIDATO");
    usuario.setHabilidades(datosRegistroCandidato.getHabilidades());
    try {
      servicioLogin.registrar(usuario);
    } catch (UsuarioExistente e) {
      return mostrarErrorRegistroCandidato(
        datosRegistroCandidato,
        "Ya existe una cuenta registrada con ese correo."
      );
    }
    identificarUsuarioEnSesion(usuario);
    return new ModelAndView(RUTA_VACANTES_RECOMENDADAS);
  }

  private String validarRegistroCandidato(DatosRegistroCandidato datos) {
    if (faltanDatosCandidato(datos)) {
      return "Completá todos los campos.";
    }
    if (!correoValido(datos.getEmail())) {
      return "Ingresá un correo electrónico válido.";
    }
    if (!datos.getPassword().equals(datos.getConfirmarPassword())) {
      return "Las contraseñas no coinciden.";
    }
    if (
      datos.getHabilidades() != null && !HABILIDADES_DISPONIBLES.containsAll(datos.getHabilidades())
    ) {
      return "Seleccioná únicamente habilidades disponibles.";
    }
    return null;
  }

  private boolean faltanDatosCandidato(DatosRegistroCandidato datos) {
    return (
      campoVacio(datos.getNombre()) ||
      campoVacio(datos.getApellido()) ||
      campoVacio(datos.getEmail()) ||
      campoVacio(datos.getPassword()) ||
      campoVacio(datos.getConfirmarPassword())
    );
  }

  @RequestMapping(path = "/validar-login", method = RequestMethod.POST)
  public ModelAndView validarLogin(
    @ModelAttribute("datosLogin") DatosLogin datosLogin,
    HttpServletRequest request
  ) {
    Usuario usuarioBuscado = servicioLogin.consultarUsuario(
      datosLogin.getEmail(),
      datosLogin.getPassword()
    );
    if (usuarioBuscado != null) {
      HttpSession session = request.getSession();
      request.changeSessionId();
      session.setAttribute("USUARIO_ID", usuarioBuscado.getId());
      session.setAttribute("EMAIL", usuarioBuscado.getEmail());
      session.setAttribute("ROL", usuarioBuscado.getRol());
      return new ModelAndView(rutaInicioSegunRol(usuarioBuscado.getRol()));
    } else {
      Map<String, Object> model = new ModelMap();
      model.put(ATRIBUTO_ERROR, "Usuario o clave incorrecta");
      datosLogin.setPassword(null);
      model.put("datosLogin", datosLogin);
      return new ModelAndView("login", model);
    }
  }

  @RequestMapping(path = "/logout", method = RequestMethod.POST)
  public ModelAndView cerrarSesion(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return new ModelAndView("redirect:/login");
  }

  public ModelAndView irAHome() {
    return new ModelAndView("home");
  }

  @RequestMapping(path = "/", method = RequestMethod.GET)
  public ModelAndView inicio() {
    return new ModelAndView("redirect:/login");
  }

  private ModelAndView mostrarErrorRegistroEmpleador(DatosRegistroEmpleador datos, String error) {
    datos.setPassword(null);
    datos.setConfirmarPassword(null);
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosRegistroEmpleador", datos);
    modelo.put(ATRIBUTO_ERROR, error);
    return new ModelAndView("registro-empleador", modelo);
  }

  private ModelAndView mostrarErrorRegistroCandidato(DatosRegistroCandidato datos, String error) {
    datos.setPassword(null);
    datos.setConfirmarPassword(null);
    Map<String, Object> modelo = new ModelMap();
    modelo.put("datosRegistroCandidato", datos);
    modelo.put("habilidadesDisponibles", HABILIDADES_DISPONIBLES);
    modelo.put(ATRIBUTO_ERROR, error);
    return new ModelAndView("registro-candidato", modelo);
  }

  private boolean campoVacio(String valor) {
    return valor == null || valor.trim().isEmpty();
  }

  private boolean correoValido(String correo) {
    return FORMATO_CORREO.matcher(correo.trim()).matches();
  }

  private String rutaInicioSegunRol(String rol) {
    return "EMPLEADOR".equalsIgnoreCase(rol)
      ? RUTA_BUSQUEDAS_EMPLEADOR
      : RUTA_VACANTES_RECOMENDADAS;
  }

  private void identificarUsuarioEnSesion(Usuario usuario) {
    ServletRequestAttributes atributos =
      (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = atributos.getRequest();
    HttpSession session = request.getSession();
    request.changeSessionId();
    session.setAttribute("USUARIO_ID", usuario.getId());
    session.setAttribute("EMAIL", usuario.getEmail());
    session.setAttribute("ROL", usuario.getRol());
  }
}
