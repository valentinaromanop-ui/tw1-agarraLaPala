package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioPago;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
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

  private static final String ATRIBUTO_DATOS_PAGO = "datosPago";
  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private final ServicioPago servicioPago;

  @Autowired
  public ControladorPago(ServicioPago servicioPago) {
    this.servicioPago = servicioPago;
  }

  @RequestMapping(path = "/premium/pagar", method = RequestMethod.GET)
  public ModelAndView mostrarPago(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
    }
    Map<String, Object> modelo = new HashMap<>();
    modelo.put(ATRIBUTO_DATOS_PAGO, new DatosPago());
    modelo.put("premiumHasta", servicioPago.fechaFinPremium(usuarioId));
    return new ModelAndView("pago-premium", modelo);
  }

  @RequestMapping(path = "/premium/pagar", method = RequestMethod.POST)
  public ModelAndView pagar(
    @ModelAttribute(ATRIBUTO_DATOS_PAGO) DatosPago datos,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
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
      modelo.put(ATRIBUTO_DATOS_PAGO, datos);
      modelo.put("error", excepcion.getMessage());
      return new ModelAndView("pago-premium", modelo);
    }
  }

  @RequestMapping(path = "/premium/empleador/pagar", method = RequestMethod.GET)
  public ModelAndView mostrarPagoEmpleador(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioEmpleador(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
    }
    return vistaPagoEmpleador(new DatosPago(), servicioPago.fechaFinPremium(usuarioId), null);
  }

  @RequestMapping(path = "/premium/empleador/pagar", method = RequestMethod.POST)
  public ModelAndView pagarEmpleador(
    @ModelAttribute(ATRIBUTO_DATOS_PAGO) DatosPago datos,
    HttpServletRequest request
  ) {
    Long usuarioId = obtenerUsuarioEmpleador(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
    }
    try {
      servicioPago.pagarEmpleador(
        usuarioId,
        datos.getTitular(),
        datos.getNumero(),
        datos.getVencimiento(),
        datos.getCvv()
      );
      return new ModelAndView("redirect:/premium/empleador/pagar?premium=true");
    } catch (IllegalArgumentException excepcion) {
      return vistaPagoEmpleador(
        datos,
        servicioPago.fechaFinPremium(usuarioId),
        excepcion.getMessage()
      );
    }
  }

  @RequestMapping(path = "/pagos", method = RequestMethod.GET)
  public ModelAndView historial(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView(REDIRECT_LOGIN);
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

  private Long obtenerUsuarioEmpleador(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (
      session == null ||
      !ROL_EMPLEADOR.equalsIgnoreCase(String.valueOf(session.getAttribute("ROL")))
    ) {
      return null;
    }
    Object id = session.getAttribute("USUARIO_ID");
    return id instanceof Number ? ((Number) id).longValue() : null;
  }

  private String rutaPagoSegunRol(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !(session.getAttribute("USUARIO_ID") instanceof Number)) {
      return REDIRECT_LOGIN;
    }
    String rol = String.valueOf(session.getAttribute("ROL"));
    if (ROL_EMPLEADOR.equalsIgnoreCase(rol)) {
      return "redirect:/premium/empleador/pagar";
    }
    return esPostulante(rol) ? "redirect:/premium/pagar" : REDIRECT_LOGIN;
  }

  private ModelAndView vistaPagoEmpleador(
    DatosPago datos,
    LocalDateTime premiumHasta,
    String error
  ) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put(ATRIBUTO_DATOS_PAGO, datos);
    modelo.put("premiumHasta", premiumHasta);
    modelo.put("precioPlanEmpleador", ServicioPago.PRECIO_PREMIUM_EMPLEADOR);
    if (error != null) {
      modelo.put("error", error);
    }
    return new ModelAndView("pago-premium-empleador", modelo);
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }
}
