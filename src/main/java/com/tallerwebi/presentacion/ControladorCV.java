package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller // le dice a Spring que esta clase atiende pedidos web.
public class ControladorCV {

  private final ServicioCV servicioCV;
  private static final String SIN_HABILIDADES = "Sin habilidades adicionales";
  private final ServicioPerfil servicioPerfil;
  private final ServicioIA servicioIA;

  public ControladorCV(
    ServicioCV servicioCV,
    ServicioPerfil servicioPerfil,
    ServicioIA servicioIA
  ) {
    this.servicioCV = servicioCV;
    this.servicioPerfil = servicioPerfil;
    this.servicioIA = servicioIA;
  }

  @RequestMapping(path = "/cv", method = RequestMethod.POST)
  public ModelAndView subirCV(
    @RequestParam("cv") MultipartFile archivo,
    HttpServletRequest request
  ) throws Exception {
    byte[] contenido = archivo.getBytes();
    if (!servicioCV.esArchivoValido(archivo.getOriginalFilename(), contenido)) {
      return vistaHome("error", "Formato de archivo no válido");
    }
    try {
      String texto = servicioCV.extraerTexto(contenido);
      String cv = servicioIA.generarCvAts(texto, armarHabilidades(request));
      ModelAndView vista = vistaHome("mensaje", "Archivo subido correctamente");
      vista.addObject("cvProcesado", cv);
      return vista;
    } catch (Exception e) {
      return vistaHome("error", "No se pudo generar el CV. Intentá de nuevo más tarde.");
    }
  }

  private String armarHabilidades(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    if (!(id instanceof Number)) {
      return SIN_HABILIDADES;
    }
    PerfilPostulante perfil = servicioPerfil.obtenerPerfil(((Number) id).longValue());
    if (perfil == null) {
      return SIN_HABILIDADES;
    }
    List<String> nombres = new ArrayList<>();
    for (PostulanteSkill ps : perfil.getPostulanteSkills()) {
      nombres.add(ps.getSkill().getNombre());
    }
    return nombres.isEmpty() ? SIN_HABILIDADES : String.join(", ", nombres);
  }

  private ModelAndView vistaHome(String clave, String valor) {
    Map<String, Object> modelo = new ModelMap();
    modelo.put(clave, valor);
    return new ModelAndView("home", modelo);
  }
}
