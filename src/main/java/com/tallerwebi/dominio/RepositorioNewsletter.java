package com.tallerwebi.dominio;

public interface RepositorioNewsletter {
  SuscriptorNewsletter buscarPorEmail(String email);
  void guardar(SuscriptorNewsletter suscriptor);
}
