package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.VacanteDTO;
import java.util.List;
import java.util.Optional;

public class VacanteResultadoBusqueda {

  private final String titulo;
  private final String empresa;
  private final String ubicacion;
  private final String modalidad;
  private final List<String> skills;
  private final String url;
  private final String fechaPublicacion;

  public VacanteResultadoBusqueda(VacanteDTO vacante) {
    titulo = vacante.getTitulo();
    empresa = vacante.getEmpresa();
    ubicacion = vacante.getUbicacion();
    modalidad = vacante.getCondiciones().getModalidad();
    skills = vacante.getSkills();
    url = vacante.getUrl();
    // El formato ISO evita necesitar un módulo JSON adicional para LocalDateTime.
    fechaPublicacion =
      Optional.ofNullable(vacante.getFechaPublicacion()).map(Object::toString).orElse("");
  }

  public String getTitulo() {
    return titulo;
  }

  public String getEmpresa() {
    return empresa;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public String getModalidad() {
    return modalidad;
  }

  public List<String> getSkills() {
    return skills;
  }

  public String getUrl() {
    return url;
  }

  public String getFechaPublicacion() {
    return fechaPublicacion;
  }
}
