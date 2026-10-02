package com.tallerwebi.dominio;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service // le avisa a Spring que esta clase es parte de la lógica del negocio y que la tiene que crear él cuando arranque la app. Sin esa etiqueta, el controlador no podría pedirla. Al poner @Service encima de una clase, le indicás a Spring que la detecte y la gestione como un servicio: una clase que suele contener lógica de negocio y puede ser inyectada en otras clases, como controladores o repositorios.//
public class ServicioCVImpl implements ServicioCV {

  @Override
  public boolean esFormatoValido(String nombreArchivo) {
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
      return false;
    }
    if (nombreArchivo.lastIndexOf(".") == -1) {
      return false;
    }
    String extension = nombreArchivo
      .substring(nombreArchivo.lastIndexOf(".") + 1)
      .toLowerCase(Locale.ROOT);
    return ("pdf".equals(extension) || "doc".equals(extension) || "docx".equals(extension));
  }
}
