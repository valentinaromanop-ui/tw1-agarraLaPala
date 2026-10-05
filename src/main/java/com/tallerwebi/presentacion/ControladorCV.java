package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ServicioCV;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller // le dice a Spring que esta clase atiende pedidos web.
public class ControladorCV {

  private final ServicioCV servicioCV;

  public ControladorCV(ServicioCV servicioCV) {
    this.servicioCV = servicioCV;
  }

  @RequestMapping(path = "/cv", method = RequestMethod.POST) //"este método atiende los pedidos POST a /cv", que es lo que manda el formulario.
  public ModelAndView subirCV(@RequestParam("cv") MultipartFile archivo) {
    servicioCV.esFormatoValido(archivo.getOriginalFilename());

    Map<String, Object> modelo = new ModelMap();
    modelo.put("mensaje", "Archivo subido correctamente");
    return new ModelAndView("home", modelo);
  }
}
