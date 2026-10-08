package com.tallerwebi.dominio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
public class GeneradorPdfAts {

  private static final float MARGEN = 48;
  private static final float TAMANIO_FUENTE = 10;
  private static final float INTERLINEADO = 14;

  public byte[] generar(String contenido) throws IOException {
    try (
      PDDocument documento = new PDDocument();
      ByteArrayOutputStream salida = new ByteArrayOutputStream()
    ) {
      PDFont fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      List<String> lineas = ajustarLineas(contenido, fuente);
      int lineasPorPagina = (int) ((PDRectangle.A4.getHeight() - 2 * MARGEN) / INTERLINEADO);

      for (int inicio = 0; inicio < lineas.size(); inicio += lineasPorPagina) {
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        try (PDPageContentStream stream = new PDPageContentStream(documento, pagina)) {
          stream.beginText();
          stream.setFont(fuente, TAMANIO_FUENTE);
          stream.newLineAtOffset(MARGEN, PDRectangle.A4.getHeight() - MARGEN);
          int fin = Math.min(inicio + lineasPorPagina, lineas.size());
          for (int indice = inicio; indice < fin; indice++) {
            stream.showText(convertirCaracteres(lineas.get(indice), fuente));
            stream.newLineAtOffset(0, -INTERLINEADO);
          }
          stream.endText();
        }
      }

      documento.save(salida);
      return salida.toByteArray();
    }
  }

  private List<String> ajustarLineas(String contenido, PDFont fuente) throws IOException {
    List<String> lineas = new ArrayList<>();
    float anchoMaximo = PDRectangle.A4.getWidth() - 2 * MARGEN;
    String texto = contenido == null ? "" : contenido;

    for (String parrafo : texto.split("\\R", -1)) {
      if (parrafo.isBlank()) {
        lineas.add("");
      } else {
        ajustarParrafo(parrafo, lineas, fuente, anchoMaximo);
      }
    }
    return lineas.isEmpty() ? List.of("") : lineas;
  }

  private void ajustarParrafo(
    String parrafo,
    List<String> lineas,
    PDFont fuente,
    float anchoMaximo
  ) throws IOException {
    StringBuilder linea = new StringBuilder();
    for (String palabra : parrafo.trim().split("\\s+")) {
      if (linea.isEmpty()) {
        agregarPalabra(palabra, linea, lineas, fuente, anchoMaximo);
      } else if (ancho(fuente, linea + " " + palabra) <= anchoMaximo) {
        linea.append(' ').append(palabra);
      } else {
        lineas.add(linea.toString());
        linea.setLength(0);
        agregarPalabra(palabra, linea, lineas, fuente, anchoMaximo);
      }
    }
    if (!linea.isEmpty()) {
      lineas.add(linea.toString());
    }
  }

  private void agregarPalabra(
    String palabra,
    StringBuilder linea,
    List<String> lineas,
    PDFont fuente,
    float anchoMaximo
  ) throws IOException {
    if (ancho(fuente, palabra) <= anchoMaximo) {
      linea.append(palabra);
    } else {
      dividirPalabra(palabra, linea, lineas, fuente, anchoMaximo);
    }
  }

  private void dividirPalabra(
    String palabra,
    StringBuilder linea,
    List<String> lineas,
    PDFont fuente,
    float anchoMaximo
  ) throws IOException {
    for (int codePoint : palabra.codePoints().toArray()) {
      String caracter = new String(Character.toChars(codePoint));
      if (!linea.isEmpty() && ancho(fuente, linea + caracter) > anchoMaximo) {
        lineas.add(linea.toString());
        linea.setLength(0);
      }
      linea.append(caracter);
    }
  }

  private float ancho(PDFont fuente, String texto) throws IOException {
    return (fuente.getStringWidth(convertirCaracteres(texto, fuente)) * TAMANIO_FUENTE) / 1000;
  }

  private String convertirCaracteres(String texto, PDFont fuente) throws IOException {
    StringBuilder convertido = new StringBuilder();
    for (int codePoint : texto.codePoints().toArray()) {
      String caracter = new String(Character.toChars(codePoint));
      try {
        fuente.encode(caracter);
        convertido.append(caracter);
      } catch (IllegalArgumentException e) {
        convertido.append('?');
      }
    }
    return convertido.toString();
  }
}
