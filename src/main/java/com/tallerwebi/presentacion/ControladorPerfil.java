package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.PerfilPostulante;
import com.tallerwebi.dominio.PostulanteSkill;
import com.tallerwebi.dominio.ServicioPerfil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPerfil {

  private static final Pattern TELEFONO_VALIDO = Pattern.compile("^[0-9]+$");
  private static final String RUTA_PERFIL = "/perfil";
  private static final String ATRIBUTO_PERFIL = "perfil";
  private static final String VISTA_PERFIL = "perfil";
  private static final List<String> IDIOMAS_DISPONIBLES = List.of(
    "Español",
    "Inglés",
    "Portugués",
    "Francés",
    "Italiano",
    "Alemán",
    "Japonés",
    "Mandarín"
  );

  private final ServicioPerfil servicioPerfil;

  @Autowired
  public ControladorPerfil(ServicioPerfil servicioPerfil) {
    this.servicioPerfil = servicioPerfil;
  }

  @RequestMapping(path = RUTA_PERFIL, method = RequestMethod.GET)
  public ModelAndView mostrarPerfil(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    PerfilPostulante perfil = servicioPerfil.obtenerPerfil(usuarioId);
    if (perfil == null) {
      perfil = new PerfilPostulante();
    } else {
      for (PostulanteSkill postulanteSkill : perfil.getPostulanteSkills()) {
        perfil.getSkillIds().add(postulanteSkill.getSkill().getId());
      }
    }
    Map<String, Object> modelo = new HashMap<>();
    modelo.put(ATRIBUTO_PERFIL, perfil);
    modelo.put("skills", servicioPerfil.obtenerSkills());
    modelo.put("idiomasDisponibles", IDIOMAS_DISPONIBLES);
    if (request.getParameter("guardado") != null) {
      modelo.put("mensaje", "Perfil guardado");
    }
    return new ModelAndView(VISTA_PERFIL, modelo);
  }

  @RequestMapping(path = RUTA_PERFIL + "/guardar", method = RequestMethod.POST)
  public ModelAndView guardarPerfil(
    @ModelAttribute(ATRIBUTO_PERFIL) PerfilPostulante datos,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    if (!telefonoValido(datos.getTelefono())) {
      return mostrarPerfilConError(datos, "El teléfono debe contener únicamente números.");
    }
    servicioPerfil.guardarPerfil(usuarioId, datos);
    return new ModelAndView("redirect:" + RUTA_PERFIL + "?guardado=true");
  }

  private ModelAndView mostrarPerfilConError(PerfilPostulante perfil, String error) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put(ATRIBUTO_PERFIL, perfil);
    modelo.put("skills", servicioPerfil.obtenerSkills());
    modelo.put("idiomasDisponibles", IDIOMAS_DISPONIBLES);
    modelo.put("error", error);
    return new ModelAndView(VISTA_PERFIL, modelo);
  }

  private Long obtenerUsuarioPostulante(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !esPostulante(session.getAttribute("ROL"))) {
      return null;
    }
    Object id = session.getAttribute("USUARIO_ID");
    if (id instanceof Number) {
      return ((Number) id).longValue();
    }
    return null;
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }

  private boolean telefonoValido(String telefono) {
    return telefono == null || telefono.isEmpty() || TELEFONO_VALIDO.matcher(telefono).matches();
  }
}
