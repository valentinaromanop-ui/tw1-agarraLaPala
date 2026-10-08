package com.tallerwebi.dominio;

public class CurriculumGratisYaGeneradoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CurriculumGratisYaGeneradoException() {
    super("El CV gratuito ya fue generado para esta cuenta.");
  }
}
