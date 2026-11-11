package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ArchivoCvPostulanteDTO;
import com.tallerwebi.dominio.CupoPostulacionesCompletoException;
import com.tallerwebi.dominio.EstadoPostulacion;
import com.tallerwebi.dominio.FotoPerfilPostulanteDTO;
import com.tallerwebi.dominio.OfertaCerradaException;
import com.tallerwebi.dominio.PerfilPostulanteDTO;
import com.tallerwebi.dominio.PostulanteOfertaDTO;
import com.tallerwebi.dominio.ServicioInvitacion;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.TipoVistaPerfil;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.VacanteNoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
@SuppressWarnings("PMD.CyclomaticComplexity")
public class ControladorPostulacion {

  private static final String RUTA_POSTULACIONES = "/postulaciones";
  private static final String FILTRO_TODOS = "TODOS";
  private static final String FILTRO_PENDIENTE = "PENDIENTE";
  private static final String OFERTA_NO_VALIDA = "La oferta no es válida.";
  private static final String PATH_VARIABLE_OFERTA_ID = "ofertaId";
  private static final String PATH_VARIABLE_POSTULACION_ID = "postulacionId";
  private static final String RUTA_CV_POSTULANTE =
    "/ofertas/{ofertaId}/postulantes/{postulacionId}/cv";
  private static final String RUTA_PERFIL_POSTULANTE =
    "/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil";
  private static final String RUTA_FOTO_PERFIL_POSTULANTE = RUTA_PERFIL_POSTULANTE + "/foto";
  private final ServicioPostulacion servicioPostulacion;
  private final ServicioOfertaEmpleador servicioOfertaEmpleador;
  private final ServicioPago servicioPago;
  private ServicioInvitacion servicioInvitacion;

  @Autowired
  public ControladorPostulacion(
    ServicioPostulacion servicioPostulacion,
    ServicioOfertaEmpleador servicioOfertaEmpleador,
    ServicioPago servicioPago
  ) {
    this.servicioPostulacion = servicioPostulacion;
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
    this.servicioPago = servicioPago;
  }

  @Autowired
  public void setServicioInvitacion(ServicioInvitacion servicioInvitacion) {
    this.servicioInvitacion = servicioInvitacion;
  }

  public ControladorPostulacion(ServicioPostulacion servicioPostulacion) {
    this(servicioPostulacion, null, null);
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
      registrarClickSiCorresponde(ofertaId);
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
      registrarClickSiCorresponde(idOferta);
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
    @RequestParam(value = "skill", required = false) String skillFiltro,
    @RequestParam(value = "fechaDesde", required = false) String fechaDesde,
    @RequestParam(value = "fechaHasta", required = false) String fechaHasta,
    @RequestParam(value = "soloPremium", defaultValue = "false") boolean soloPremium,
    HttpSession session
  ) {
    Long empleadorId = obtenerEmpleadorId(session);
    boolean premium =
      servicioPago != null && empleadorId != null && servicioPago.esPremiumActivo(empleadorId);
    List<PostulanteOfertaDTO> todasLasPostulaciones = servicioPostulacion.obtenerPostulantes(
      vacanteId,
      empleadorId
    );
    Map<String, Object> modelo = new ModelMap();
    agregarResumenPostulantes(modelo, todasLasPostulaciones);
    modelo.put(
      "postulantes",
      filtrarPostulantes(
        todasLasPostulaciones,
        premium,
        estadoFiltro,
        skillFiltro,
        fechaDesde,
        fechaHasta,
        soloPremium,
        modelo
      )
    );
    modelo.put("premium", premium);
    var vacante = servicioPostulacion.obtenerVacante("LOCAL", vacanteId);
    modelo.put("vacante", vacante);
    if (servicioInvitacion != null && empleadorId != null && vacante != null) {
      modelo.put(
        "candidatosSugeridos",
        servicioInvitacion.obtenerCandidatosSugeridos(vacanteId, empleadorId)
      );
      modelo.put("puedeInvitar", servicioInvitacion.puedeInvitar(vacanteId, empleadorId));
    }
    agregarMetricas(modelo, vacanteId, vacante, premium);
    return new ModelAndView("postulantes-oferta", modelo);
  }

  private void agregarResumenPostulantes(
    Map<String, Object> modelo,
    List<PostulanteOfertaDTO> postulantes
  ) {
    modelo.put("cantidadPostulantes", postulantes.size());
    modelo.put("cantidadPendientes", contarEstado(postulantes, EstadoPostulacion.PENDIENTE));
    modelo.put("cantidadAceptadas", contarEstado(postulantes, EstadoPostulacion.ACEPTADA));
    modelo.put("cantidadRechazadas", contarEstado(postulantes, EstadoPostulacion.RECHAZADA));
  }

  private List<PostulanteOfertaDTO> filtrarPostulantes(
    List<PostulanteOfertaDTO> postulantes,
    boolean premium,
    String estadoFiltro,
    String skillFiltro,
    String fechaDesde,
    String fechaHasta,
    boolean soloPremium,
    Map<String, Object> modelo
  ) {
    String estado = premium ? normalizarFiltroEstado(estadoFiltro) : FILTRO_TODOS;
    LocalDate desde = null;
    LocalDate hasta = null;
    String errorFiltro = null;
    if (premium) {
      try {
        desde = parsearFecha(fechaDesde);
        hasta = parsearFecha(fechaHasta);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
          errorFiltro = "La fecha desde no puede ser posterior a la fecha hasta.";
        }
      } catch (DateTimeParseException excepcion) {
        errorFiltro = "Ingresá las fechas con un formato válido.";
      }
    }
    modelo.put("estadoFiltro", estado);
    modelo.put("skillFiltro", skillFiltro);
    modelo.put("fechaDesde", fechaDesde);
    modelo.put("fechaHasta", fechaHasta);
    modelo.put("soloPremium", premium && soloPremium);
    if (errorFiltro != null) {
      modelo.put("errorFiltro", errorFiltro);
    }
    return filtrarPorValores(
      postulantes,
      estado,
      premium ? skillFiltro : null,
      desde,
      hasta,
      premium && soloPremium,
      errorFiltro != null
    );
  }

  private List<PostulanteOfertaDTO> filtrarPorValores(
    List<PostulanteOfertaDTO> postulantes,
    String estado,
    String skillFiltro,
    LocalDate desde,
    LocalDate hasta,
    boolean soloPremium,
    boolean filtroFechaInvalido
  ) {
    String skill = skillFiltro == null ? "" : skillFiltro.trim().toLowerCase(Locale.ROOT);
    return postulantes
      .stream()
      .filter(postulante ->
        FILTRO_TODOS.equals(estado) ||
        estadoPendiente(postulante.getEstado(), estado) ||
        postulante.getEstado().name().equals(estado)
      )
      .filter(postulante ->
        skill.isEmpty() ||
        postulante
          .getHabilidades()
          .stream()
          .anyMatch(habilidad -> habilidad.toLowerCase(Locale.ROOT).contains(skill))
      )
      .filter(postulante -> !soloPremium || postulante.isPremium())
      .filter(postulante ->
        filtroFechaInvalido ||
        desde == null ||
        !postulante.getFechaPostulacion().toLocalDate().isBefore(desde)
      )
      .filter(postulante ->
        filtroFechaInvalido ||
        hasta == null ||
        !postulante.getFechaPostulacion().toLocalDate().isAfter(hasta)
      )
      .toList();
  }

  private void agregarMetricas(
    Map<String, Object> modelo,
    Long vacanteId,
    com.tallerwebi.dominio.Vacante vacante,
    boolean premium
  ) {
    if (servicioOfertaEmpleador != null && vacante != null && premium) {
      modelo.put("vistas", servicioOfertaEmpleador.obtenerVistas(vacanteId));
      modelo.put("clicksPostular", servicioOfertaEmpleador.obtenerClicksPostular(vacanteId));
      modelo.put(
        "cantidadPostulacionesOferta",
        servicioOfertaEmpleador.obtenerPostulaciones(vacanteId)
      );
    }
  }

  private LocalDate parsearFecha(String fecha) {
    return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha);
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
    return postulantes
      .stream()
      .filter(postulante ->
        estado == EstadoPostulacion.PENDIENTE
          ? estadoPendiente(postulante.getEstado(), FILTRO_PENDIENTE)
          : postulante.getEstado() == estado
      )
      .count();
  }

  private boolean estadoPendiente(EstadoPostulacion estado, String filtro) {
    return (
      FILTRO_PENDIENTE.equals(filtro) &&
      (estado == EstadoPostulacion.PENDIENTE ||
        estado == EstadoPostulacion.ENVIADA ||
        estado == EstadoPostulacion.EN_REVISION ||
        estado == EstadoPostulacion.ENTREVISTA ||
        estado == EstadoPostulacion.REGISTRADA)
    );
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
    servicioPostulacion.registrarVistaPostulante(
      ofertaId,
      postulacionId,
      empleadorId,
      TipoVistaPerfil.CV
    );
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
    servicioPostulacion.registrarVistaPostulante(
      ofertaId,
      postulacionId,
      empleadorId,
      TipoVistaPerfil.PERFIL
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
    modelo.put("esPremium", servicioPago != null && servicioPago.esPremiumActivo(usuarioId));
    return new ModelAndView("postulaciones", modelo);
  }

  private void registrarClickSiCorresponde(String ofertaId) {
    if (servicioOfertaEmpleador == null || ofertaId == null || !ofertaId.startsWith("LOCAL:")) {
      return;
    }
    try {
      Long vacanteId = Long.valueOf(ofertaId.substring("LOCAL:".length()));
      var vacante = servicioPostulacion.obtenerVacante("LOCAL", vacanteId);
      if (vacante != null && vacante.getEmpleadorId() != null) {
        servicioOfertaEmpleador.registrarClickPostular(vacanteId);
      }
    } catch (NumberFormatException excepcion) {
      return;
    }
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
