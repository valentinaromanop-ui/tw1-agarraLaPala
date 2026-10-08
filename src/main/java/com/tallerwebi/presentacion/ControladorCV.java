package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ArchivoCvInvalidoException;
import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.CurriculumGratisYaGeneradoException;
import com.tallerwebi.dominio.ServicioCurriculumGenerado;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorCV {

  private static final String ERROR_CV = "errorCV";

  private final ServicioCurriculumGenerado servicioCurriculum;

  @Autowired
  public ControladorCV(ServicioCurriculumGenerado servicioCurriculum) {
    this.servicioCurriculum = servicioCurriculum;
  }

  @PostMapping({ "/cv/subir", "/cv/generar", "/cv" })
  public ModelAndView subirOriginal(
    @RequestParam(value = "archivo", required = false) MultipartFile archivo,
    @RequestParam(value = "cv", required = false) MultipartFile cvAnterior,
    HttpSession session,
    RedirectAttributes redirectAttributes
  ) {
    Long usuarioId = obtenerUsuarioId(session);
    if (usuarioId == null) {
      return redirigirConError(
        redirectAttributes,
        "No pude verificar tu sesión. Volvé a iniciar sesión como postulante y probá nuevamente."
      );
    }
    if (!esPostulante(session)) {
      return redirigirConError(
        redirectAttributes,
        "La generación de CV está disponible para cuentas postulantes."
      );
    }
    return procesarGeneracion(usuarioId, archivo, cvAnterior, redirectAttributes);
  }

  private ModelAndView procesarGeneracion(
    Long usuarioId,
    MultipartFile archivo,
    MultipartFile cvAnterior,
    RedirectAttributes redirectAttributes
  ) {
    try {
      MultipartFile archivoCv = obtenerArchivo(archivo, cvAnterior);
      servicioCurriculum.subirOriginal(
        usuarioId,
        archivoCv.getOriginalFilename(),
        archivoCv.getBytes()
      );
      return new ModelAndView("redirect:/perfil?cvSubido=true");
    } catch (ArchivoCvInvalidoException | CurriculumGratisYaGeneradoException e) {
      return redirigirConError(redirectAttributes, e.getMessage());
    } catch (IllegalArgumentException e) {
      return redirigirConError(
        redirectAttributes,
        "No se encontró una cuenta de postulante válida."
      );
    } catch (IOException | IllegalStateException e) {
      return redirigirConError(
        redirectAttributes,
        "No se pudo guardar el CV. Revisá el archivo e intentá nuevamente."
      );
    }
  }

  @PostMapping("/cv/ats/generar")
  public ModelAndView generarAts(HttpSession session, RedirectAttributes redirectAttributes) {
    Long usuarioId = obtenerPostulanteId(session);
    if (usuarioId == null) {
      return redirigirConError(
        redirectAttributes,
        "No pude verificar tu sesión. Volvé a iniciar sesión como postulante y probá nuevamente."
      );
    }
    try {
      servicioCurriculum.generarAts(usuarioId);
      return new ModelAndView("redirect:/perfil?cvGenerado=true");
    } catch (CurriculumGratisYaGeneradoException e) {
      return redirigirConErrorPerfil(redirectAttributes, e.getMessage());
    } catch (IllegalArgumentException | IllegalStateException e) {
      return redirigirConErrorPerfil(redirectAttributes, e.getMessage());
    }
  }

  private MultipartFile obtenerArchivo(MultipartFile archivo, MultipartFile cvAnterior) {
    MultipartFile archivoCv = archivo != null ? archivo : cvAnterior;
    if (archivoCv == null) {
      throw new ArchivoCvInvalidoException();
    }
    return archivoCv;
  }

  private ModelAndView redirigirConError(RedirectAttributes redirectAttributes, String mensaje) {
    redirectAttributes.addFlashAttribute(ERROR_CV, mensaje);
    return new ModelAndView("redirect:/home");
  }

  private ModelAndView redirigirConErrorPerfil(
    RedirectAttributes redirectAttributes,
    String mensaje
  ) {
    redirectAttributes.addFlashAttribute(ERROR_CV, mensaje);
    return new ModelAndView("redirect:/perfil");
  }

  @GetMapping("/cv/descargar")
  public ResponseEntity<byte[]> descargar(HttpSession session) {
    return responderConArchivoAts(session, true);
  }

  @GetMapping("/cv/ats/ver")
  public ResponseEntity<byte[]> verAts(HttpSession session) {
    return responderConArchivoAts(session, false);
  }

  @GetMapping("/cv/ats/descargar")
  public ResponseEntity<byte[]> descargarAts(HttpSession session) {
    return responderConArchivoAts(session, true);
  }

  @GetMapping("/cv/original/ver")
  public ResponseEntity<byte[]> verOriginal(HttpSession session) {
    return responderConArchivoOriginal(session, false);
  }

  @GetMapping("/cv/original/descargar")
  public ResponseEntity<byte[]> descargarOriginal(HttpSession session) {
    return responderConArchivoOriginal(session, true);
  }

  private ResponseEntity<byte[]> responderConArchivoAts(HttpSession session, boolean descargar) {
    Long usuarioId = obtenerPostulanteId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      CurriculumGenerado curriculum = servicioCurriculum.obtener(usuarioId);
      if (curriculum == null || curriculum.getArchivoAtsPdf() == null) {
        return ResponseEntity.notFound().build();
      }
      String disposition = descargar ? "attachment" : "inline";
      MultiValueMap<String, String> headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
      headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.builder(disposition).filename("curriculum-ats.pdf").build().toString()
      );
      return new ResponseEntity<>(curriculum.getArchivoAtsPdf(), headers, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private ResponseEntity<byte[]> responderConArchivoOriginal(
    HttpSession session,
    boolean descargar
  ) {
    Long usuarioId = obtenerPostulanteId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    CurriculumGenerado curriculum = servicioCurriculum.obtener(usuarioId);
    if (curriculum == null || curriculum.getArchivoOriginal() == null) {
      return ResponseEntity.notFound().build();
    }
    String nombre = curriculum.getNombreArchivoOriginal();
    String extension = extension(nombre);
    String contentType = "pdf".equals(extension)
      ? MediaType.APPLICATION_PDF_VALUE
      : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String disposition = descargar || !"pdf".equals(extension) ? "attachment" : "inline";
    MultiValueMap<String, String> headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, contentType);
    headers.add(
      HttpHeaders.CONTENT_DISPOSITION,
      ContentDisposition.builder(disposition).filename(nombre).build().toString()
    );
    return new ResponseEntity<>(curriculum.getArchivoOriginal(), headers, HttpStatus.OK);
  }

  private String extension(String nombre) {
    int punto = nombre.lastIndexOf('.');
    return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(java.util.Locale.ROOT);
  }

  private Long obtenerPostulanteId(HttpSession session) {
    if (!esPostulante(session)) {
      return null;
    }
    return obtenerUsuarioId(session);
  }

  private Long obtenerUsuarioId(HttpSession session) {
    if (session == null) {
      return null;
    }
    Object id = session.getAttribute("USUARIO_ID");
    return id instanceof Number ? ((Number) id).longValue() : null;
  }

  private boolean esPostulante(HttpSession session) {
    if (session == null) {
      return false;
    }
    Object rol = session.getAttribute("ROL");
    if (
      !"CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) &&
      !"POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    ) {
      return false;
    }
    return true;
  }
}
