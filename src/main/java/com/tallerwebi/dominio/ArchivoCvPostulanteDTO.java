package com.tallerwebi.dominio;

import java.util.Objects;

public class ArchivoCvPostulanteDTO {

  private final String nombreArchivo;
  private final byte[] contenido;

  public ArchivoCvPostulanteDTO(String nombreArchivo, byte[] contenido) {
    this.nombreArchivo = Objects.requireNonNull(nombreArchivo);
    this.contenido = Objects.requireNonNull(contenido).clone();
  }

  public String getNombreArchivo() {
    return nombreArchivo;
  }

  public byte[] getContenido() {
    return contenido.clone();
  }
}
