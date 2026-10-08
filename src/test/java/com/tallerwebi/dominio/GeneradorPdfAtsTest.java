package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class GeneradorPdfAtsTest {

  private final GeneradorPdfAts generador = new GeneradorPdfAts();

  @Test
  void generaPdfConTextoEnEspanol() throws IOException {
    byte[] pdf = generador.generar("Experiencia\nDesarrollé aplicaciones en español.");

    try (PDDocument documento = Loader.loadPDF(pdf)) {
      String extraido = new PDFTextStripper().getText(documento);
      assertThat(extraido, containsString("Experiencia"));
      assertThat(extraido, containsString("español"));
    }
  }

  @Test
  void creaPaginasAdicionalesCuandoElTextoEsLargo() throws IOException {
    String contenido = "Experiencia profesional. ".repeat(1000);

    try (PDDocument documento = Loader.loadPDF(generador.generar(contenido))) {
      assertThat(documento.getNumberOfPages(), greaterThanOrEqualTo(2));
    }
  }
}
