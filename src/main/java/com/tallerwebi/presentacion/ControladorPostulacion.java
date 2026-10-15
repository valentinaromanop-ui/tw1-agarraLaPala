package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.CupoPostulacionesCompletoException;
import com.tallerwebi.dominio.OfertaCerradaException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPostulacion {

  private static final String RUTA_POSTULACIONES = "/postulaciones";
  private static final String OFERTA_NO_VALIDA = "La oferta no es válida.";
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
  public ModelAndView mostrarPostulantes(@PathVariable("id") Long vacanteId, HttpSession session) {
    Object rol = session.getAttribute("ROL");
    Object id = session.getAttribute("USUARIO_ID");
    if (!"EMPLEADOR".equalsIgnoreCase(String.valueOf(rol)) || !(id instanceof Number)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    Long empleadorId = ((Number) id).longValue();
    Map<String, Object> modelo = new ModelMap();
    modelo.put("postulantes", servicioPostulacion.obtenerPostulantes(vacanteId, empleadorId));
    modelo.put("vacante", servicioPostulacion.obtenerVacante("LOCAL", vacanteId));
    return new ModelAndView("postulantes-oferta", modelo);
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
