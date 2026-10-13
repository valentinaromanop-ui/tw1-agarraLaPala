package com.tallerwebi.dominio;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class ServicioCorreoTest {

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

    servicio.enviar(
      new Correo(List.of("uno@gmail.com", "dos@gmail.com"), "Vacante", "Ver oferta")
    );

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
