package com.tallerwebi.dominio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service // le avisa a Spring que esta clase es parte de la lógica del negocio y que la tiene que crear él cuando arranque la app. Sin esa etiqueta, el controlador no podría pedirla. Al poner @Service encima de una clase, le indicás a Spring que la detecte y la gestione como un servicio: una clase que suele contener lógica de negocio y puede ser inyectada en otras clases, como controladores o repositorios.//
public class ServicioCVImpl implements ServicioCV {

  private static final String WORD_DOCUMENT_PATH = "word/document.xml";
  private static final String CONTENT_TYPES_PATH = "[Content_Types].xml";
  private static final String WORD_DOCUMENT_TYPE =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
  private static final String PDF_EXTENSION = "pdf";
  private static final String DOC_EXTENSION = "doc";
  private static final String DOCX_EXTENSION = "docx";
  private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] DOC_SIGNATURE = {
    (byte) 0xD0,
    (byte) 0xCF,
    0x11,
    (byte) 0xE0,
    (byte) 0xA1,
    (byte) 0xB1,
    0x1A,
    (byte) 0xE1,
  };

  @Override
  public boolean esFormatoValido(String nombreArchivo) {
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
      return false;
    }
    if (nombreArchivo.lastIndexOf(".") == -1) {
      return false;
    }
    if (nombreArchivo.startsWith(".")) {
      return false;
    }

    String extension = nombreArchivo
      .substring(nombreArchivo.lastIndexOf(".") + 1)
      .toLowerCase(Locale.ROOT);
    return (
      PDF_EXTENSION.equals(extension) ||
      DOC_EXTENSION.equals(extension) ||
      DOCX_EXTENSION.equals(extension)
    );
  }

  @Override
  public boolean esArchivoValido(String nombreArchivo, byte[] contenido) throws IOException {
    if (!esFormatoValido(nombreArchivo) || contenido == null || contenido.length == 0) {
      return false;
    }

    String extension = nombreArchivo
      .substring(nombreArchivo.lastIndexOf(".") + 1)
      .toLowerCase(Locale.ROOT);

    if (PDF_EXTENSION.equals(extension)) {
      return empiezaCon(contenido, PDF_SIGNATURE);
    }
    if (DOC_EXTENSION.equals(extension)) {
      return empiezaCon(contenido, DOC_SIGNATURE);
    }
    return DOCX_EXTENSION.equals(extension) && esDocumentoWordOpenXml(contenido);
  }

  @Override
  public String extraerTexto(byte[] contenido) throws Exception {
    if (empiezaCon(contenido, PDF_SIGNATURE)) {
      try (PDDocument pdf = Loader.loadPDF(contenido)) {
        return limpiarTexto(new PDFTextStripper().getText(pdf));
      }
    }
    try (
      XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(contenido));
      XWPFWordExtractor extractor = new XWPFWordExtractor(docx)
    ) {
      return limpiarTexto(extractor.getText());
    }
  }

  private boolean empiezaCon(byte[] contenido, byte[] firma) {
    if (contenido.length < firma.length) {
      return false;
    }
    for (int i = 0; i < firma.length; i++) {
      if (contenido[i] != firma[i]) {
        return false;
      }
    }
    return true;
  }

  private String limpiarTexto(String texto) {
    return texto.replaceAll("\\r\\n|\\r", "\n").replaceAll("\n{3,}", "\n\n").trim();
  }

  private boolean esDocumentoWordOpenXml(byte[] contenido) throws IOException {
    boolean tieneDocumentoWord = false;
    boolean tieneTipoWord = false;

    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
      while (true) {
        ZipEntry entrada = zip.getNextEntry();
        if (entrada == null) {
          break;
        }
        if (ServicioCVImpl.WORD_DOCUMENT_PATH.equals(entrada.getName())) {
          tieneDocumentoWord = true;
        } else if (ServicioCVImpl.CONTENT_TYPES_PATH.equals(entrada.getName())) {
          String tipos = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
          tieneTipoWord = tipos.contains(ServicioCVImpl.WORD_DOCUMENT_TYPE);
        }
      }
    } catch (ZipException e) {
      return false;
    }
    return tieneDocumentoWord && tieneTipoWord;
  }
}
