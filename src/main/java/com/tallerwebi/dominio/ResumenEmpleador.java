package com.tallerwebi.dominio;

public class ResumenEmpleador {

  private final long busquedasActivas;
  private final long busquedasCerradas;
  private final long postulacionesRecibidas;

  public ResumenEmpleador(
    long busquedasActivas,
    long busquedasCerradas,
    long postulacionesRecibidas
  ) {
    this.busquedasActivas = busquedasActivas;
    this.busquedasCerradas = busquedasCerradas;
    this.postulacionesRecibidas = postulacionesRecibidas;
  }

  public long getBusquedasActivas() {
    return busquedasActivas;
  }

  public long getBusquedasCerradas() {
    return busquedasCerradas;
  }

  public long getPostulacionesRecibidas() {
    return postulacionesRecibidas;
  }
}
