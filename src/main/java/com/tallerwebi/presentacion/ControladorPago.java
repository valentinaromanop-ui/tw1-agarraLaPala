package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioPago;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorPago {

  private final ServicioPago servicioPago;

  @Autowired
  public ControladorPago(ServicioPago servicioPago) {
    this.servicioPago = servicioPago;
  }

  @RequestMapping(path = "/premium/pagar", method = RequestMethod.GET)
  public ModelAndView mostrarPago(HttpServletRequest request) {
    if (obtenerUsuarioPostulante(request) == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("datosPago", new DatosPago());
    modelo.put("premiumHasta", servicioPago.fechaFinPremium(obtenerUsuarioPostulante(request)));
    return new ModelAndView("pago-premium", modelo);
  }

  @RequestMapping(path = "/premium/pagar", method = RequestMethod.POST)
  public ModelAndView pagar(
    @ModelAttribute("datosPago") DatosPago datos,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    try {
      servicioPago.pagar(
        usuarioId,
        datos.getTitular(),
        datos.getNumero(),
        datos.getVencimiento(),
        datos.getCvv()
      );
      return new ModelAndView("redirect:/premium/pagar?premium=true");
    } catch (IllegalArgumentException excepcion) {
      Map<String, Object> modelo = new HashMap<>();
      modelo.put("datosPago", datos);
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView("pago-premium", modelo);
    }
  }

  @RequestMapping(path = "/pagos", method = RequestMethod.GET)
  public ModelAndView historial(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView("redirect:/login");
    }
    Map<String, Object> modelo = new HashMap<>();
    modelo.put("pagos", servicioPago.listarPagos(usuarioId));
    return new ModelAndView("pagos", modelo);
  }

  private Long obtenerUsuarioPostulante(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !esPostulante(session.getAttribute("ROL"))) {
      return null;
    }
    Object id = session.getAttribute("USUARIO_ID");
    return id instanceof Number ? ((Number) id).longValue() : null;
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }
}
