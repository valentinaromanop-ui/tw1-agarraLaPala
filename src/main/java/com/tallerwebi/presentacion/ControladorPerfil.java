package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorPerfil {

  private static final Logger LOGGER = Logger.getLogger(ControladorPerfil.class.getName());
  private static final Pattern TELEFONO_VALIDO = Pattern.compile("^[0-9]+$");
  private static final String RUTA_PERFIL = "/perfil";
  private static final String ATRIBUTO_PERFIL = "perfil";
  private static final String VISTA_PERFIL = "perfil";
  private static final String ATRIBUTO_USUARIO_ID = "USUARIO_ID";
  private static final String PREFIJO_REDIRECT = "redirect:";
  private static final String REDIRECT_LOGIN = "redirect:/login";
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
  private final ServicioCurriculumGenerado servicioCurriculum;
  private final ServicioPago servicioPago;
  private static final Set<String> TIPOSFOTO = Set.of("image/jpeg", "image/png");
  private static final String mensaje = "mensaje";
  private static final String error = "error";

  @PostMapping("/perfil/foto")
  public String subirFotoPerfil(
    @RequestParam("foto") MultipartFile foto,
    HttpSession session,
    RedirectAttributes redirect
  ) {
    Long usuarioId = (Long) session.getAttribute(ATRIBUTO_USUARIO_ID);
    String redirect1 = PREFIJO_REDIRECT + RUTA_PERFIL;
    if (usuarioId == null) {
      redirect.addFlashAttribute(error, "Debes iniciar sesión para subir una foto de perfil.");
      return REDIRECT_LOGIN;
    }

    if (foto.isEmpty()) {
      redirect.addFlashAttribute(error, "No se ha seleccionado ninguna foto.");
      return redirect1;
    }

    if (!TIPOSFOTO.contains(foto.getContentType())) {
      redirect.addFlashAttribute(
        error,
        "El tipo de archivo no es válido. Solo se permiten imágenes JPEG o PNG."
      );
      return redirect1;
    }

    try {
      servicioPerfil.guardarFotoPerfil(usuarioId, foto.getBytes(), foto.getContentType());
      redirect.addFlashAttribute(mensaje, "Foto de perfil subida correctamente.");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error al subir la foto de perfil", e);
      redirect.addFlashAttribute(error, "Ocurrió un error al subir la foto de perfil.");
    }

    return PREFIJO_REDIRECT + RUTA_PERFIL;
  }

  @PostMapping("/perfil/foto/eliminar")
  public String eliminarFotoPerfil(HttpSession session, RedirectAttributes redirect) {
    Long usuarioId = (Long) session.getAttribute(ATRIBUTO_USUARIO_ID);
    if (usuarioId == null) {
      redirect.addFlashAttribute(error, "Debes iniciar sesión para eliminar tu foto de perfil.");
      return REDIRECT_LOGIN;
    }

    servicioPerfil.eliminarFotoPerfil(usuarioId);
    redirect.addFlashAttribute(mensaje, "Foto de perfil eliminada.");
    return PREFIJO_REDIRECT + RUTA_PERFIL;
  }

  @GetMapping("/perfil/foto")
  public ResponseEntity<byte[]> verFoto(HttpSession session) {
    Long usuarioId = (Long) session.getAttribute(ATRIBUTO_USUARIO_ID);
    Usuario usuario = servicioPerfil.obtenerUsuario(usuarioId);
    if (usuario == null || !usuario.tieneFotoPerfil()) return ResponseEntity.notFound().build(); //indica imagen no disp
    return ResponseEntity
      .ok() //respuesta exitosa
      .contentType(MediaType.parseMediaType(usuario.getFotoPerfilTipo())) //le dice a navegador el tipo de img
      .cacheControl(CacheControl.noCache()) //el navegador  no guarda la foto en caché.
      .body(usuario.getFotoPerfil()); //coloca datos binarios de la foto en el objeto usuario
  }

  @Autowired
  public ControladorPerfil(
    ServicioPerfil servicioPerfil,
    ServicioCurriculumGenerado servicioCurriculum,
    ServicioPago servicioPago
  ) {
    this.servicioPerfil = servicioPerfil;
    this.servicioCurriculum = servicioCurriculum;
    this.servicioPago = servicioPago;
  }

  public ControladorPerfil(
    ServicioPerfil servicioPerfil,
    ServicioCurriculumGenerado servicioCurriculum
  ) {
    this(servicioPerfil, servicioCurriculum, null);
  }

  @RequestMapping(path = ControladorPerfil.RUTA_PERFIL, method = RequestMethod.GET)
  public ModelAndView mostrarPerfil(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    PerfilPostulante perfil = servicioPerfil.obtenerPerfil(usuarioId);
    if (perfil == null) {
      perfil = new PerfilPostulante();
    }
    Map<String, Object> modelo = new HashMap<>();
    Usuario usuario = servicioPerfil.obtenerUsuario(usuarioId);
    modelo.put("tieneFotoPerfil", usuario != null && usuario.tieneFotoPerfil());
    modelo.put(ATRIBUTO_PERFIL, perfil);
    modelo.put("skills", servicioPerfil.obtenerSkills());
    modelo.put("idiomasDisponibles", IDIOMAS_DISPONIBLES);
    modelo.put("curriculumGenerado", servicioCurriculum.obtener(usuarioId));
    modelo.put(
      "premiumHasta",
      servicioPago == null ? null : servicioPago.fechaFinPremium(usuarioId)
    );
    if (request.getParameter("guardado") != null) {
      modelo.put(
        mensaje,
        "Perfil guardado correctamente. Tus selecciones quedaron asociadas a tu cuenta."
      );
    }
    if (request.getParameter("cvSubido") != null) {
      modelo.put(mensaje, "CV original guardado en tu perfil.");
    }
    if (request.getParameter("cvGenerado") != null) {
      modelo.put(
        mensaje,
        "Tu versión ATS está guardada en PDF y podés verla o descargarla desde acá."
      );
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
      return new ModelAndView(REDIRECT_LOGIN);
    }
    if (!telefonoValido(datos.getTelefono())) {
      return mostrarPerfilConError(
        usuarioId,
        datos,
        "El teléfono debe contener únicamente números."
      );
    }
    servicioPerfil.guardarPerfil(usuarioId, datos);
    return new ModelAndView(PREFIJO_REDIRECT + RUTA_PERFIL + "?guardado=true");
  }

  private ModelAndView mostrarPerfilConError(
    Long usuarioId,
    PerfilPostulante perfil,
    String error
  ) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put(ATRIBUTO_PERFIL, perfil);
    modelo.put("skills", servicioPerfil.obtenerSkills());
    modelo.put("idiomasDisponibles", IDIOMAS_DISPONIBLES);
    modelo.put("curriculumGenerado", servicioCurriculum.obtener(usuarioId));
    modelo.put("error", error);
    return new ModelAndView(VISTA_PERFIL, modelo);
  }

  private Long obtenerUsuarioPostulante(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !esPostulante(session.getAttribute("ROL"))) {
      return null;
    }
    Object id = session.getAttribute(ATRIBUTO_USUARIO_ID);
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
