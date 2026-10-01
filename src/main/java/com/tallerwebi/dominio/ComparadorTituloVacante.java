package com.tallerwebi.dominio;

import java.util.Comparator;

public class ComparadorTituloVacante implements Comparator<String> {

  @Override
  public int compare(String primerTitulo, String segundoTitulo) {
    return primerTitulo.compareToIgnoreCase(segundoTitulo);
  }
}
