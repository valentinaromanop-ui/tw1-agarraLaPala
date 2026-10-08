package com.tallerwebi.dominio;

public class ArchivoCvInvalidoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ArchivoCvInvalidoException() {
    super("El archivo no es un documento PDF, DOC o DOCX válido.");
  }
}
