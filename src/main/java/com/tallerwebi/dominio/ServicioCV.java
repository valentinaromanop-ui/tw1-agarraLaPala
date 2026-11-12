package com.tallerwebi.dominio;

import java.util.Collection;
import java.util.List;

public interface ServicioCV {
  boolean esFormatoValido(String nombreArchivo);

  boolean esArchivoValido(String nombreArchivo, byte[] contenido) throws java.io.IOException;
  String extraerTexto(byte[] contenido) throws java.io.IOException;
  List<String> extraerHabilidades(String texto, Collection<String> habilidadesDisponibles);
  List<String> extraerPalabrasClave(String texto);
}
