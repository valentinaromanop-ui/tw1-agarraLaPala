package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class ServicioCorreoImplTest {

  @Test
  void enviaBienvenidaEnFormatoHtml() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    MimeMessage mensaje = new MimeMessage(Session.getInstance(new java.util.Properties()));
    when(sender.createMimeMessage()).thenReturn(mensaje);
    ServicioCorreoImpl servicio = new ServicioCorreoImpl(sender, "app@correo.com", true);

    servicio.enviar(new Correo(List.of("persona@correo.com"), "Bienvenida", "<h1>Hola</h1>", true));

    verify(sender).send(mensaje);
    assertEquals("<h1>Hola</h1>", mensaje.getContent());
    assertTrue(mensaje.getDataHandler().getContentType().startsWith("text/html"));
  }

  @Test
  void enviaElCorreoConLosDatosIndicados() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ServicioCorreoImpl servicio = new ServicioCorreoImpl(sender, "app@correo.com", true);

    servicio.enviar(new Correo(List.of("uno@gmail.com"), "Vacante", "Ver oferta"));

    verify(sender).send(crearMensaje("uno@gmail.com"));
  }

  @Test
  void noEnviaSiEstaDeshabilitado() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ServicioCorreoImpl servicio = new ServicioCorreoImpl(sender, "app@correo.com", false);

    servicio.enviar(new Correo(List.of("uno@gmail.com"), "Vacante", "Ver oferta"));

    verify(sender, never()).send(any(SimpleMailMessage.class));
  }

  @Test
  void continuaConElSiguienteSiFallaUnEnvio() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ServicioCorreoImpl servicio = new ServicioCorreoImpl(sender, "app@correo.com", true);
    SimpleMailMessage primero = crearMensaje("uno@gmail.com");
    SimpleMailMessage segundo = crearMensaje("dos@gmail.com");
    doThrow(new MailSendException("Error SMTP")).when(sender).send(primero);

    servicio.enviar(new Correo(List.of("uno@gmail.com", "dos@gmail.com"), "Vacante", "Ver oferta"));

    verify(sender).send(primero);
    verify(sender).send(segundo);
  }

  private SimpleMailMessage crearMensaje(String destinatario) {
    SimpleMailMessage mensaje = new SimpleMailMessage();
    mensaje.setFrom("app@correo.com");
    mensaje.setTo(destinatario);
    mensaje.setSubject("Vacante");
    mensaje.setText("Ver oferta");
    return mensaje;
  }
}
