package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.RepositorioUsuario;
import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.ServicioReporte;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin")
public class ControladorAdmin {

  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String REDIRECT_ADMIN = "redirect:/admin";

  private final ServicioReporte servicio;
  private final RepositorioUsuario repoUsuario;
  private final RepositorioVacante repoVacante;

  @Autowired
  public ControladorAdmin(
    ServicioReporte servicioReporte,
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante
  ) {
    this.servicio = servicioReporte;
    this.repoUsuario = repositorioUsuario;
    this.repoVacante = repositorioVacante;
  }

  private boolean noEsAdmin(HttpSession sesion) {
    return !"ADMIN".equals(sesion.getAttribute("ROL"));
  }

  @GetMapping
  @Transactional(readOnly = true)
  public ModelAndView panel(HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return new ModelAndView(REDIRECT_LOGIN);
    }
    ModelAndView modelo = new ModelAndView("admin");
    modelo.addObject("reportes", servicio.listarPendientes());
    modelo.addObject("usuarios", repoUsuario.listarNoAdmin());
    modelo.addObject("ofertas", repoVacante.buscarOfertasPublicadasPorEmpleadores());
    return modelo;
  }

  @PostMapping("/reportes/{id}/bloquear-contenido")
  public String bc(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.bloquearContenido(id);
    return REDIRECT_ADMIN;
  }

  @PostMapping("/reportes/{id}/bloquear-usuario")
  @Transactional
  public String bu(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.bloquearUsuario(id);
    return REDIRECT_ADMIN;
  }

  @PostMapping("/reportes/{id}/descartar")
  public String d(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.descartar(id);
    return REDIRECT_ADMIN;
  }

  @Transactional
  @PostMapping("/usuarios/{id}/bloquear")
  public String ub(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.bloquearUsuarioDirecto(id);
    return REDIRECT_ADMIN;
  }

  @Transactional
  @PostMapping("/usuarios/{id}/desbloquear")
  public String ud(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.desbloquearUsuario(id);
    return REDIRECT_ADMIN;
  }

  @PostMapping("/ofertas/{id}/ocultar")
  public String oo(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.cambiarEstadoOferta(id, false);
    return REDIRECT_ADMIN;
  }

  @Transactional
  @PostMapping("/ofertas/{id}/mostrar")
  public String om(@PathVariable("id") Long id, HttpSession sesion) {
    if (noEsAdmin(sesion)) {
      return REDIRECT_LOGIN;
    }
    servicio.cambiarEstadoOferta(id, true);
    return REDIRECT_ADMIN;
  }
}
