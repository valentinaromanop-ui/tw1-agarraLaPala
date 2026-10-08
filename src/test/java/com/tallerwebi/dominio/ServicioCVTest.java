package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioCVTest {

  private ServicioCVImpl servicio;

  @BeforeEach
  public void init() {
    this.servicio = new ServicioCVImpl();
  }

  @Test
  public void esmiCVpdfesUnFormatoValido() {
    boolean resultado = servicio.esFormatoValido("miCV.pdf");
    assertThat(resultado, equalTo(true));
  }

  @Test
  public void esMiCvExeUnFormatoInvalido() {
    boolean resultado = servicio.esFormatoValido("miCV.exe");
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaFallarSiMiCVesNulo() {
    boolean resultado = servicio.esFormatoValido(null);
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaFallarSiMiCVEsUnNombreVacio() {
    boolean resultado = servicio.esFormatoValido("");
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaFallarSiMiCVNoTieneExtension() {
    boolean resultado = servicio.esFormatoValido("miCV");
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaFallarSiMiCVSeLlamaPDF() {
    boolean resultado = servicio.esFormatoValido("pdf");
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaFallarSiMiCVSeLlamaPuntoPDF() {
    boolean resultado = servicio.esFormatoValido(".pdf");
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaAceptarContenidoConFirmaPDF() throws IOException {
    byte[] contenido = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
    boolean resultado = servicio.esArchivoValido("miCV.pdf", contenido);
    assertThat(resultado, equalTo(true));
  }

  @Test
  public void deberiaRechazarUnPDFConContenidoQueNoEsPDF() throws IOException {
    byte[] contenido = "esto no es un PDF".getBytes(StandardCharsets.US_ASCII);
    boolean resultado = servicio.esArchivoValido("miCV.pdf", contenido);
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaRechazarUnPDFSiElContenidoEsDeOtroTipo() throws IOException {
    byte[] contenido = new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
    boolean resultado = servicio.esArchivoValido("miCV.pdf", contenido);
    assertThat(resultado, equalTo(false));
  }

  @Test
  public void deberiaAceptarContenidoConFirmaDeWordAntiguo() throws IOException {
    byte[] contenido = {
      (byte) 0xD0,
      (byte) 0xCF,
      0x11,
      (byte) 0xE0,
      (byte) 0xA1,
      (byte) 0xB1,
      0x1A,
      (byte) 0xE1,
    };
    assertThat(servicio.esArchivoValido("miCV.doc", contenido), equalTo(true));
    assertThat(servicio.esArchivoValido("miCV.pdf", contenido), equalTo(false));
  }

  @Test
  public void deberiaAceptarUnDocumentoWordOpenXml() throws IOException {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(salida)) {
      zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
      zip.write(
        ("<Types><Override ContentType=\"application/vnd.openxmlformats-officedocument." +
          "wordprocessingml.document.main+xml\"/></Types>").getBytes(StandardCharsets.UTF_8)
      );
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("word/document.xml"));
      zip.write("<document/>".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    assertThat(servicio.esArchivoValido("miCV.docx", salida.toByteArray()), equalTo(true));
    assertThat(servicio.esArchivoValido("miCV.pdf", salida.toByteArray()), equalTo(false));
  }

  @Test
  public void deberiaDevolverTextoAlEnviarCVEnFormatoDocx() throws Exception {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    try (XWPFDocument docx = new XWPFDocument()) {
      docx.createParagraph().createRun().setText("Hola CV");
      docx.write(salida);
    }
    String texto = servicio.extraerTexto(salida.toByteArray());
    assertThat(texto, containsString("Hola CV"));
  }

  @Test
  public void deberiaDevolverTextoAlEnviarCVEnFormatoPdf() throws Exception {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    PDPage pagina = new PDPage();
    try (PDDocument pdf = new PDDocument()) {
      pdf.addPage(pagina);
      try (PDPageContentStream contenido = new PDPageContentStream(pdf, pagina)) {
        contenido.beginText();
        contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        contenido.newLineAtOffset(50, 700);
        contenido.showText("Hola CV");
        contenido.endText();
      }
      pdf.save(salida);
    }
    String texto = servicio.extraerTexto(salida.toByteArray());
    assertThat(texto, containsString("Hola CV"));
  }
}
