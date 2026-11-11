package com.tallerwebi.dominio;

import java.time.Duration;
import java.time.LocalDateTime;

public class VistaPerfilDTO {

  private final String empresa;
  private final String ofertaTitulo;
  private final TipoVistaPerfil tipo;
  private final LocalDateTime fecha;

  public VistaPerfilDTO(VistaPerfil vista) {
    empresa = vista.getEmpresa();
    ofertaTitulo = vista.getOfertaTitulo();
    tipo = vista.getTipo();
    fecha = vista.getFecha();
  }

  public String getEmpresa() {
    return empresa;
  }

  public String getOfertaTitulo() {
    return ofertaTitulo;
  }

  public TipoVistaPerfil getTipo() {
    return tipo;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public String getEtiquetaTipo() {
    return tipo == TipoVistaPerfil.CV ? "CV visto" : "Perfil visto";
  }

  public String getFechaRelativa() {
    long dias = Duration.between(fecha, LocalDateTime.now()).toDays();
    return dias == 0 ? "hoy" : "hace " + dias + (dias == 1 ? " día" : " días");
  }
}
