package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioPago;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@Controller
public class ControladorPago {

  private static final String ATRIBUTO_DATOS_PAGO = "datosPago";
  private static final String REDIRECT_LOGIN = "redirect:/login";
  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
    LocalDateTime premiumHasta = servicioPago.fechaFinPremium(usuarioId);
    modelo.put("premiumHasta", premiumHasta);
    modelo.put("mostrarFormularioPago", premiumHasta == null || extender(request));
    return new ModelAndView("pago-premium", modelo);
  }

  @RequestMapping(path = "/premium/pagar", method = RequestMethod.POST)
  public ModelAndView pagar(
    @ModelAttribute(ATRIBUTO_DATOS_PAGO) DatosPago datos,
    HttpServletRequest request,
    RedirectAttributes mensajes
  ) {
    Long usuarioId = obtenerUsuarioPostulante(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
    }
    try {
      LocalDateTime vence = servicioPago
        .pagar(
          usuarioId,
          datos.getTitular(),
          datos.getNumero(),
          datos.getVencimiento(),
          datos.getCvv()
        )
        .getFechaFin();
      mensajes.addFlashAttribute(
        "mensaje",
        "¡Ya sos Premium! Disfrutá tus ventajas hasta " + FORMATO_FECHA.format(vence)
      );
      return new ModelAndView("redirect:/perfil");
    } catch (IllegalArgumentException excepcion) {
      limpiarDatosSensibles(datos);
      Map<String, Object> modelo = new HashMap<>();
      modelo.put(ATRIBUTO_DATOS_PAGO, datos);
      modelo.put("error", excepcion.getMessage());
      modelo.put("premiumHasta", servicioPago.fechaFinPremium(usuarioId));
      modelo.put("mostrarFormularioPago", true);
      return new ModelAndView("pago-premium", modelo);
    }
  }

  public ModelAndView pagar(DatosPago datos, HttpServletRequest request) {
    return pagar(datos, request, new RedirectAttributesModelMap());
  }

  @RequestMapping(path = "/premium/empleador/pagar", method = RequestMethod.GET)
  public ModelAndView mostrarPagoEmpleador(HttpServletRequest request) {
    Long usuarioId = obtenerUsuarioEmpleador(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
    }
    LocalDateTime premiumHasta = servicioPago.fechaFinPremium(usuarioId);
    return vistaPagoEmpleador(
      new DatosPago(),
      premiumHasta,
      null,
      premiumHasta == null || extender(request)
    );
  }

  @RequestMapping(path = "/premium/empleador/pagar", method = RequestMethod.POST)
  public ModelAndView pagarEmpleador(
    @ModelAttribute(ATRIBUTO_DATOS_PAGO) DatosPago datos,
    HttpServletRequest request,
    RedirectAttributes mensajes
  ) {
    Long usuarioId = obtenerUsuarioEmpleador(request);
    if (usuarioId == null) {
      return new ModelAndView(rutaPagoSegunRol(request));
    }
    try {
      LocalDateTime vence = servicioPago
        .pagarEmpleador(
          usuarioId,
          datos.getTitular(),
          datos.getNumero(),
          datos.getVencimiento(),
          datos.getCvv()
        )
        .getFechaFin();
      mensajes.addFlashAttribute(
        "mensaje",
        "¡Ya sos Premium! Disfrutá tus ventajas hasta " + FORMATO_FECHA.format(vence)
      );
      return new ModelAndView("redirect:/empleador/perfil");
    } catch (IllegalArgumentException excepcion) {
      limpiarDatosSensibles(datos);
      return vistaPagoEmpleador(
        datos,
        servicioPago.fechaFinPremium(usuarioId),
        excepcion.getMessage(),
        true
      );
    }
  }

  public ModelAndView pagarEmpleador(DatosPago datos, HttpServletRequest request) {
    return pagarEmpleador(datos, request, new RedirectAttributesModelMap());
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
    String error,
    boolean mostrarFormularioPago
  ) {
    Map<String, Object> modelo = new HashMap<>();
    modelo.put(ATRIBUTO_DATOS_PAGO, datos);
    modelo.put("premiumHasta", premiumHasta);
    modelo.put("mostrarFormularioPago", mostrarFormularioPago);
    modelo.put("precioPlanEmpleador", ServicioPago.PRECIO_PREMIUM_EMPLEADOR);
    if (error != null) {
      modelo.put("error", error);
    }
    return new ModelAndView("pago-premium-empleador", modelo);
  }

  private boolean extender(HttpServletRequest request) {
    return "true".equalsIgnoreCase(request.getParameter("extender"));
  }

  private void limpiarDatosSensibles(DatosPago datos) {
    datos.setNumero("");
    datos.setCvv("");
  }

  private boolean esPostulante(Object rol) {
    return (
      "CANDIDATO".equalsIgnoreCase(String.valueOf(rol)) ||
      "POSTULANTE".equalsIgnoreCase(String.valueOf(rol))
    );
  }
}
