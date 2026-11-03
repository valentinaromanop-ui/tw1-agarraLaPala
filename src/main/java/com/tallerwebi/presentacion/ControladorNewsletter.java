package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioNewsletter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorNewsletter {

  private final ServicioNewsletter servicioNewsletter;
  private static final String RESULTADO = "fragments/footer :: resultadoNewsletter";

  @Autowired
  public ControladorNewsletter(ServicioNewsletter servicioNewsletter) {
    this.servicioNewsletter = servicioNewsletter;
  }

  @RequestMapping(path = "/newsletter", method = RequestMethod.GET)
  public ModelAndView mostrarNewsletter() {
    return new ModelAndView("redirect:/home");
  }

  @RequestMapping(path = "/newsletter", method = RequestMethod.POST)
  public ModelAndView suscribir(@ModelAttribute("datosNewsletter") DatosNewsletter datos) {
    Map<String, Object> modelo = new ModelMap();
    try {
      boolean nueva = servicioNewsletter.suscribir(datos.getEmail());
      String mensaje = nueva
        ? "¡Gracias! Tu suscripción al newsletter quedó registrada."
        : "Este correo ya está suscrito al newsletter.";
      modelo.put("mensajeNewsletter", mensaje);
    } catch (IllegalArgumentException excepcion) {
      modelo.put("errorNewsletter", excepcion.getMessage());
    }
    return new ModelAndView(RESULTADO, modelo);
  }
}
