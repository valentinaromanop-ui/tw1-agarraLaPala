package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.EmpleoGuardado;
import com.tallerwebi.dominio.ServicioEmpleoGuardado;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorEmpleosGuardados {

  private static final String USUARIO_ID = "USUARIO_ID";
  private static final String ID_OFERTA = "idOferta";

  private final ServicioEmpleoGuardado servicioEmpleoGuardado;

  @Autowired
  public ControladorEmpleosGuardados(ServicioEmpleoGuardado servicioEmpleoGuardado) {
    this.servicioEmpleoGuardado = servicioEmpleoGuardado;
  }

  @GetMapping("/empleos-guardados")
  public ModelAndView mostrarEmpleosGuardados(HttpSession session) {
    Long usuarioId = usuarioId(session);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new ModelMap();
    modelo.put("empleosGuardados", servicioEmpleoGuardado.listar(usuarioId));
    return new ModelAndView("empleos-guardados", modelo);
  }

  @GetMapping("/empleos-guardados/ids")
  @ResponseBody
  public ResponseEntity<?> listarIds(HttpSession session) {
    Long usuarioId = usuarioId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
    }
    return ResponseEntity.ok(servicioEmpleoGuardado.listarIds(usuarioId));
  }

  @PostMapping("/empleos-guardados")
  @ResponseBody
  public ResponseEntity<?> guardar(
    @RequestParam(ID_OFERTA) String idOferta,
    @RequestParam(
      name = "titulo",
      required = false,
      defaultValue = "Oferta sin título"
    ) String titulo,
    @RequestParam(
      name = "empresa",
      required = false,
      defaultValue = "Empresa no informada"
    ) String empresa,
    @RequestParam(
      name = "ubicacion",
      required = false,
      defaultValue = "Ubicación no informada"
    ) String ubicacion,
    @RequestParam(
      name = "modalidad",
      required = false,
      defaultValue = "Modalidad no informada"
    ) String modalidad,
    @RequestParam(name = "url", required = false, defaultValue = "") String url,
    HttpSession session
  ) {
    Long usuarioId = usuarioId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
    }
    try {
      EmpleoGuardado datos = new EmpleoGuardado();
      datos.setIdOferta(idOferta);
      datos.setTitulo(titulo);
      datos.setEmpresa(empresa);
      datos.setUbicacion(ubicacion);
      datos.setModalidad(modalidad);
      datos.setUrl(url);
      EmpleoGuardado guardado = servicioEmpleoGuardado.guardar(usuarioId, datos);
      return ResponseEntity.ok(Map.of("ok", true, "idOferta", guardado.getIdOferta()));
    } catch (IllegalArgumentException excepcion) {
      return ResponseEntity
        .badRequest()
        .body(Map.of("ok", false, "mensaje", excepcion.getMessage()));
    }
  }

  @PostMapping("/empleos-guardados/quitar")
  @ResponseBody
  public ResponseEntity<?> quitar(@RequestParam(ID_OFERTA) String idOferta, HttpSession session) {
    Long usuarioId = usuarioId(session);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
    }
    try {
      servicioEmpleoGuardado.quitar(usuarioId, idOferta);
      return ResponseEntity.ok(Map.of("ok", true, ID_OFERTA, idOferta));
    } catch (IllegalArgumentException excepcion) {
      return ResponseEntity
        .badRequest()
        .body(Map.of("ok", false, "mensaje", excepcion.getMessage()));
    }
  }

  private Long usuarioId(HttpSession session) {
    Object id = session.getAttribute(USUARIO_ID);
    return id instanceof Number ? ((Number) id).longValue() : null;
  }
}
