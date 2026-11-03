package com.tallerwebi.dominio;

@FunctionalInterface
public interface ServicioNewsletter {
  boolean suscribir(String email);
}
