package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ArchivoCvPostulanteDTO;
import com.tallerwebi.dominio.CupoPostulacionesCompletoException;
import com.tallerwebi.dominio.EstadoPostulacion;
import com.tallerwebi.dominio.FotoPerfilPostulanteDTO;
import com.tallerwebi.dominio.OfertaCerradaException;
import com.tallerwebi.dominio.PerfilPostulanteDTO;
import com.tallerwebi.dominio.PostulanteOfertaDTO;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.VacanteNoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorPostulacion {

  private static final String RUTA_POSTULACIONES = "/postulaciones";
  private static final String FILTRO_TODOS = "TODOS";
  private static final String OFERTA_NO_VALIDA = "La oferta no es válida.";
  private static final String PATH_VARIABLE_OFERTA_ID = "ofertaId";
  private static final String PATH_VARIABLE_POSTULACION_ID = "postulacionId";
  private static final String RUTA_CV_POSTULANTE =
    "/ofertas/{ofertaId}/postulantes/{postulacionId}/cv";
  private static final String RUTA_PERFIL_POSTULANTE =
    "/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil";
  private static final String RUTA_FOTO_PERFIL_POSTULANTE = RUTA_PERFIL_POSTULANTE + "/foto";
  private final ServicioPostulacion servicioPostulacion;

  @Autowired
  public ControladorPostulacion(ServicioPostulacion servicioPostulacion) {
    this.servicioPostulacion = servicioPostulacion;
  }

  @RequestMapping(path = "/postularse", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity<Map<String, Object>> postularse(
    @RequestParam("ofertaId") String ofertaId,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(mensaje("Iniciá sesión como postulante."));
    }
    try {
      servicioPostulacion.crearPostulacion(usuarioId, ofertaId);
      return ResponseEntity.ok(mensaje("Postulación registrada"));
    } catch (OfertaCerradaException excepcion) {
      return respuesta(HttpStatus.BAD_REQUEST, false, excepcion.getMessage());
    } catch (CupoPostulacionesCompletoException excepcion) {
      return respuesta(HttpStatus.CONFLICT, false, excepcion.getMessage());
    } catch (VacanteNoEncontradaException excepcion) {
      return respuesta(HttpStatus.NOT_FOUND, false, excepcion.getMessage());
    } catch (IllegalArgumentException excepcion) {
      return respuesta(HttpStatus.BAD_REQUEST, false, excepcion.getMessage());
    }
  }

  @RequestMapping(
    path = RUTA_POSTULACIONES,
    method = RequestMethod.POST,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  public ResponseEntity<Map<String, Object>> registrar(
    @RequestParam(value = "idOferta", required = false, defaultValue = "") String idOferta,
    @RequestParam(value = "titulo", required = false, defaultValue = "") String titulo,
    @RequestParam(value = "empresa", required = false, defaultValue = "") String empresa,
    @RequestParam(value = "ubicacion", required = false, defaultValue = "") String ubicacion,
    @RequestParam(value = "modalidad", required = false, defaultValue = "") String modalidad,
    @RequestParam(value = "url", required = false, defaultValue = "") String url,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return respuesta(HttpStatus.UNAUTHORIZED, false, "Iniciá sesión como postulante.");
    }
    try {
      servicioPostulacion.registrarEnVacante(
        usuarioId,
        crearOferta(idOferta, titulo, empresa, ubicacion, modalidad, url)
      );
      return respuesta(HttpStatus.OK, true, "Postulación registrada");
    } catch (OfertaCerradaException excepcion) {
      return respuesta(HttpStatus.BAD_REQUEST, false, excepcion.getMessage());
    } catch (CupoPostulacionesCompletoException excepcion) {
      return respuesta(HttpStatus.CONFLICT, false, excepcion.getMessage());
    } catch (VacanteNoEncontradaException excepcion) {
      return respuesta(HttpStatus.NOT_FOUND, false, excepcion.getMessage());
    } catch (IllegalArgumentException excepcion) {
      return respuesta(HttpStatus.BAD_REQUEST, false, excepcion.getMessage());
    }
  }

  @RequestMapping(path = "/postulaciones/ids", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<List<String>> obtenerIds(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(servicioPostulacion.obtenerOfertasPostuladas(usuarioId));
  }

  @RequestMapping(path = "/ofertas/{id}/postulantes", method = RequestMethod.GET)
  public ModelAndView mostrarPostulantes(
    @PathVariable("id") Long vacanteId,
    @RequestParam(value = "estado", defaultValue = FILTRO_TODOS) String estadoFiltro,
    HttpSession session
  ) {
    Long empleadorId = obtenerEmpleadorId(session);
    List<PostulanteOfertaDTO> todasLasPostulaciones = servicioPostulacion.obtenerPostulantes(
      vacanteId,
      empleadorId
    );
    long pendientes = contarEstado(todasLasPostulaciones, EstadoPostulacion.PENDIENTE);
    long aceptadas = contarEstado(todasLasPostulaciones, EstadoPostulacion.ACEPTADA);
    long rechazadas = contarEstado(todasLasPostulaciones, EstadoPostulacion.RECHAZADA);
    String filtroNormalizado = normalizarFiltroEstado(estadoFiltro);
    List<PostulanteOfertaDTO> postulantes = todasLasPostulaciones
      .stream()
      .filter(postulante ->
        FILTRO_TODOS.equals(filtroNormalizado) ||
        postulante.getEstado().name().equals(filtroNormalizado)
      )
      .toList();
    Map<String, Object> modelo = new ModelMap();
    modelo.put("postulantes", postulantes);
    modelo.put("cantidadPostulantes", todasLasPostulaciones.size());
    modelo.put("cantidadPendientes", pendientes);
    modelo.put("cantidadAceptadas", aceptadas);
    modelo.put("cantidadRechazadas", rechazadas);
    modelo.put("estadoFiltro", filtroNormalizado);
    modelo.put("vacante", servicioPostulacion.obtenerVacante("LOCAL", vacanteId));
    return new ModelAndView("postulantes-oferta", modelo);
  }

  @PostMapping("/ofertas/{ofertaId}/postulantes/{postulacionId}/aceptar")
  public ModelAndView aceptarPostulacion(
    @PathVariable(PATH_VARIABLE_OFERTA_ID) Long ofertaId,
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    return decidirPostulacion(
      ofertaId,
      postulacionId,
      EstadoPostulacion.ACEPTADA,
      "Postulante aceptado",
      session,
      mensajes
    );
  }

  @PostMapping("/ofertas/{ofertaId}/postulantes/{postulacionId}/rechazar")
  public ModelAndView rechazarPostulacion(
    @PathVariable(PATH_VARIABLE_OFERTA_ID) Long ofertaId,
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    return decidirPostulacion(
      ofertaId,
      postulacionId,
      EstadoPostulacion.RECHAZADA,
      "Postulante rechazado",
      session,
      mensajes
    );
  }

  private ModelAndView decidirPostulacion(
    Long ofertaId,
    Long postulacionId,
    EstadoPostulacion estado,
    String confirmacion,
    HttpSession session,
    RedirectAttributes mensajes
  ) {
    Long empleadorId = obtenerEmpleadorId(session);
    servicioPostulacion.cambiarEstado(ofertaId, postulacionId, empleadorId, estado);
    mensajes.addFlashAttribute("mensaje", confirmacion);
    return new ModelAndView("redirect:/ofertas/" + ofertaId + "/postulantes");
  }

  private long contarEstado(List<PostulanteOfertaDTO> postulantes, EstadoPostulacion estado) {
    return postulantes.stream().filter(postulante -> postulante.getEstado() == estado).count();
  }

  private String normalizarFiltroEstado(String estadoFiltro) {
    if (estadoFiltro == null) {
      return FILTRO_TODOS;
    }
    String filtro = estadoFiltro.trim().toUpperCase(Locale.ROOT);
    if (FILTRO_TODOS.equals(filtro) || estadoFiltroValido(filtro)) {
      return filtro;
    }
    return FILTRO_TODOS;
  }

  private boolean estadoFiltroValido(String filtro) {
    try {
      EstadoPostulacion.valueOf(filtro);
      return true;
    } catch (IllegalArgumentException excepcion) {
      return false;
    }
  }

  @RequestMapping(path = RUTA_CV_POSTULANTE, method = RequestMethod.GET)
  public ResponseEntity<byte[]> verCvPostulante(
    @PathVariable(PATH_VARIABLE_OFERTA_ID) Long ofertaId,
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    @RequestParam(value = "descargar", defaultValue = "false") boolean descargar,
    HttpSession session
  ) {
    Long empleadorId = obtenerEmpleadorId(session);
    ArchivoCvPostulanteDTO archivo = servicioPostulacion.obtenerCvPostulante(
      ofertaId,
      postulacionId,
      empleadorId
    );
    if (archivo == null) {
      return ResponseEntity.notFound().build();
    }
    String disposition = descargar ? "attachment" : "inline";
    MultiValueMap<String, String> headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, obtenerContentTypeCv(archivo.getNombreArchivo()));
    headers.add(
      HttpHeaders.CONTENT_DISPOSITION,
      ContentDisposition
        .builder(disposition)
        .filename(archivo.getNombreArchivo())
        .build()
        .toString()
    );
    return new ResponseEntity<>(archivo.getContenido(), headers, HttpStatus.OK);
  }

  @GetMapping(RUTA_PERFIL_POSTULANTE)
  public ModelAndView mostrarPerfilPostulante(
    @PathVariable(PATH_VARIABLE_OFERTA_ID) Long ofertaId,
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpSession session
  ) {
    Long empleadorId = obtenerEmpleadorId(session);
    PerfilPostulanteDTO perfil = servicioPostulacion.obtenerPerfilPostulante(
      ofertaId,
      postulacionId,
      empleadorId
    );
    Map<String, Object> modelo = new ModelMap();
    modelo.put("perfilPostulante", perfil);
    modelo.put("ofertaId", ofertaId);
    modelo.put("postulacionId", postulacionId);
    return new ModelAndView("perfil-postulante-lectura", modelo);
  }

  @GetMapping(RUTA_FOTO_PERFIL_POSTULANTE)
  public ResponseEntity<byte[]> verFotoPerfilPostulante(
    @PathVariable(PATH_VARIABLE_OFERTA_ID) Long ofertaId,
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpSession session
  ) {
    Long empleadorId = obtenerEmpleadorId(session);
    FotoPerfilPostulanteDTO foto = servicioPostulacion.obtenerFotoPerfilPostulante(
      ofertaId,
      postulacionId,
      empleadorId
    );
    if (foto == null) {
      return ResponseEntity.notFound().build();
    }
    if (
      !MediaType.IMAGE_JPEG_VALUE.equals(foto.getContentType()) &&
      !MediaType.IMAGE_PNG_VALUE.equals(foto.getContentType())
    ) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity
      .ok()
      .contentType(MediaType.parseMediaType(foto.getContentType()))
      .body(foto.getContenido());
  }

  @RequestMapping(path = RUTA_POSTULACIONES, method = RequestMethod.GET)
  public ModelAndView mostrarPostulaciones(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("postulaciones", servicioPostulacion.listarPostulaciones(usuarioId));
    return new ModelAndView("postulaciones", modelo);
  }

  private Map<String, Object> mensaje(String texto) {
    Map<String, Object> respuesta = new HashMap<>();
    respuesta.put("mensaje", texto);
    return respuesta;
  }

  private Long obtenerEmpleadorId(HttpSession session) {
    Object rol = session == null ? null : session.getAttribute("ROL");
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    if (!"EMPLEADOR".equalsIgnoreCase(String.valueOf(rol)) || !(id instanceof Number)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return ((Number) id).longValue();
  }

  private String obtenerContentTypeCv(String nombreArchivo) {
    int punto = nombreArchivo.lastIndexOf('.');
    String extension = punto < 0 ? "" : nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    switch (extension) {
      case "pdf":
        return MediaType.APPLICATION_PDF_VALUE;
      case "doc":
        return "application/msword";
      case "docx":
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      default:
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
  }

  private VacanteDTO crearOferta(
    String ofertaId,
    String titulo,
    String empresa,
    String ubicacion,
    String modalidad,
    String url
  ) {
    VacanteDTO oferta = new VacanteDTO();
    oferta.setId(parsearIdOferta(ofertaId));
    oferta.setFuente(parsearFuenteOferta(ofertaId));
    oferta.setTitulo(titulo);
    oferta.setEmpresa(empresa);
    oferta.setUbicacion(ubicacion);
    oferta.setModalidad(modalidad);
    oferta.setUrl(url);
    return oferta;
  }

  private Long parsearIdOferta(String ofertaId) {
    if (ofertaId == null) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA);
    }
    int separador = ofertaId.indexOf(':');
    if (separador <= 0 || separador == ofertaId.length() - 1) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA);
    }
    try {
      return Long.valueOf(ofertaId.substring(separador + 1));
    } catch (NumberFormatException excepcion) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA, excepcion);
    }
  }

  private String parsearFuenteOferta(String ofertaId) {
    String fuente = ofertaId.substring(0, ofertaId.indexOf(':')).toUpperCase(Locale.ROOT);
    if (!"LOCAL".equals(fuente) && !"JOBICY".equals(fuente)) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA);
    }
    return fuente;
  }

  private ResponseEntity<Map<String, Object>> respuesta(
    HttpStatus estado,
    boolean ok,
    String texto
  ) {
    Map<String, Object> respuesta = new HashMap<>();
    respuesta.put("ok", ok);
    respuesta.put("mensaje", texto);
    return ResponseEntity.status(estado).body(respuesta);
  }

  private Long obtenerUsuarioPostulante(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object rol = session == null ? null : session.getAttribute("ROL");
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    if (!esPostulante(rol) || !(id instanceof Number)) {
      return null;
    }
    return ((Number) id).longValue();
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }
}
