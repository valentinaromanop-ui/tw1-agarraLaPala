package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.EstadoVacante;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.Vacante;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorHome {

  private final ServicioOfertaEmpleador servicioOfertaEmpleador;
  private final ServicioPago servicioPago;
  private ServicioResena servicioResena;

  @Autowired
  public ControladorHome(
    ServicioOfertaEmpleador servicioOfertaEmpleador,
    ServicioPago servicioPago
  ) {
    this.servicioOfertaEmpleador = servicioOfertaEmpleador;
    this.servicioPago = servicioPago;
  }

  @Autowired(required = false)
  public void setServicioResena(ServicioResena servicioResena) {
    this.servicioResena = servicioResena;
  }

  @RequestMapping(path = "/home", method = RequestMethod.GET)
  public ModelAndView mostrarHome(HttpServletRequest request) {
    if (esEmpleador(request)) {
      return new ModelAndView("redirect:/home-empleador");
    }

    Map<String, Object> modelo = new ModelMap();
    List<Vacante> ofertasEmpleadores = servicioOfertaEmpleador.listarOfertasPublicadas();
    modelo.put("ofertasEmpleadores", ofertasEmpleadores);
    agregarResumenesResenas(modelo, ofertasEmpleadores);
    Object usuarioId = request.getSession(false) == null
      ? null
      : request.getSession(false).getAttribute("USUARIO_ID");
    if (usuarioId instanceof Number) {
      modelo.put("premiumHasta", servicioPago.fechaFinPremium(((Number) usuarioId).longValue()));
    }
    return new ModelAndView("home", modelo);
  }

  @RequestMapping(path = "/home-empleador", method = RequestMethod.GET)
  public ModelAndView mostrarHomeEmpleador(HttpServletRequest request) {
    if (!esEmpleador(request)) {
      return new ModelAndView(esPostulante(request) ? "redirect:/home" : "redirect:/login");
    }

    List<Vacante> ofertasEmpleadores = servicioOfertaEmpleador.listarOfertasPublicadas();
    List<Vacante> ofertasActivas = ofertasEmpleadores
      .stream()
      .filter(oferta -> oferta.getEstado() == EstadoVacante.ABIERTA)
      .toList();
    Map<String, Object> modelo = new ModelMap();
    modelo.put("ofertasEmpleadores", ofertasEmpleadores);
    modelo.put("ofertasActivas", ofertasActivas);
    agregarResumenesResenas(modelo, ofertasEmpleadores);
    Map<Long, Long> cantidadesPostulaciones = new LinkedHashMap<>();
    for (Vacante oferta : ofertasActivas) {
      cantidadesPostulaciones.put(oferta.getId(), 0L);
    }
    modelo.put("cantidadesPostulaciones", cantidadesPostulaciones);
    return new ModelAndView("home-empleador", modelo);
  }

  private void agregarResumenesResenas(Map<String, Object> modelo, List<Vacante> ofertas) {
    List<Long> empleadorIds = ofertas
      .stream()
      .map(Vacante::getEmpleadorId)
      .filter(id -> id != null)
      .distinct()
      .toList();
    modelo.put(
      "resenasPorEmpleador",
      EnriquecedorResenasVacantes.obtenerResumenes(empleadorIds, servicioResena)
    );
  }

  private boolean esEmpleador(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return (
      session != null && "EMPLEADOR".equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))
    );
  }

  private boolean esPostulante(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return false;
    }
    String rol = String.valueOf(session.getAttribute("ROL"));
    return "CANDIDATO".equalsIgnoreCase(rol) || "POSTULANTE".equalsIgnoreCase(rol);
  }
}
