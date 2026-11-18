package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.*;
import com.tallerwebi.dominio.Leccion;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorCursosPremium {

  private static final int LIMITE_TOP = 5;
  private static final String USUARIO_ID = "USUARIO_ID";
  private static final String REDIRIGIR_CURSOS = "redirect:/cursos";

  private final ServicioTendencias servicioTendencias;
  private final ServicioPago servicioPago;

  @Autowired
  public ControladorCursosPremium(
    ServicioTendencias servicioTendencias,
    ServicioPago servicioPago
  ) {
    this.servicioTendencias = servicioTendencias;
    this.servicioPago = servicioPago;
  }

  @GetMapping("/cursos")
  public ModelAndView catalogo(HttpSession session) {
    Long usuarioId = (Long) session.getAttribute(USUARIO_ID);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    ModelAndView vista = new ModelAndView("cursos-premium");
    vista.addObject("cursos", CatalogoCursosPremium.todos());
    vista.addObject("premium", esPremium(usuarioId));
    vista.addObject("top", servicioTendencias.obtenerTop(LIMITE_TOP));
    vista.addObject("skillsTop", nombresTop());
    return vista;
  }

  @GetMapping("/cursos/{cursoId}")
  public ModelAndView detalle(
    @PathVariable("cursoId") String cursoId,
    @RequestParam(value = "leccion", defaultValue = "0") int leccion,
    HttpSession session
  ) {
    Long usuarioId = (Long) session.getAttribute(USUARIO_ID);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    Optional<CursoPremium> curso = CatalogoCursosPremium.buscar(cursoId);
    if (curso.isEmpty()) {
      return new ModelAndView(REDIRIGIR_CURSOS);
    }
    ModelAndView vista = new ModelAndView("curso-detalle");
    vista.addObject("curso", curso.get());
    vista.addObject("premium", esPremium(usuarioId));
    vista.addObject(
      "leccionActual",
      Math.max(0, Math.min(leccion, curso.get().lecciones().size() - 1))
    );
    return vista;
  }

  private boolean esPremium(Long usuarioId) {
    return servicioPago != null && servicioPago.esPremiumActivo(usuarioId);
  }

  private List<String> nombresTop() {
    return servicioTendencias
      .obtenerTop(LIMITE_TOP)
      .stream()
      .map(habilidad -> habilidad.nombre().toLowerCase(Locale.ROOT))
      .toList();
  }
}
