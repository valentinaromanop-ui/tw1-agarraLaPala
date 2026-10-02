package com.tallerwebi.dominio;

@FunctionalInterface // le dice a Java y a PMD: "esta interfaz tiene un solo método a propósito".//
public interface ServicioCV {
  boolean esFormatoValido(String nombreArchivo);
}
