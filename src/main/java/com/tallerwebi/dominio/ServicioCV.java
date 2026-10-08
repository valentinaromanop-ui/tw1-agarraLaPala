package com.tallerwebi.dominio;

public interface ServicioCV {
  boolean esFormatoValido(String nombreArchivo);

  boolean esArchivoValido(String nombreArchivo, byte[] contenido) throws java.io.IOException;
  String extraerTexto(byte[] contenido) throws java.io.IOException;
}
