package com.tallerwebi.dominio;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ServicioCorreoImpl implements ServicioCorreo {

  private static final Logger LOGGER = Logger.getLogger(ServicioCorreoImpl.class.getName());
  private final JavaMailSender mailSender;
  private final String remitente;
  private final boolean habilitado;

  @Autowired
  public ServicioCorreoImpl(
    JavaMailSender mailSender,
    @Value("${correo.remitente:no-reply@localhost}") String remitente,
    @Value("${correo.habilitado:false}") boolean habilitado
  ) {
    this.mailSender = mailSender;
    this.remitente = remitente;
    this.habilitado = habilitado;
  }

  @Override
  @Async("ejecutorCorreos")
  public void enviar(Correo correo) {
    if (!habilitado) {
      LOGGER.info("Envío de correos deshabilitado. Configurá correo.habilitado para activarlo.");
      return;
    }
    for (String destinatario : correo.getDestinatarios()) {
      SimpleMailMessage mensaje = new SimpleMailMessage();
      mensaje.setFrom(remitente);
      mensaje.setTo(destinatario);
      mensaje.setSubject(correo.getAsunto());
      mensaje.setText(correo.getMensaje());
      try {
        mailSender.send(mensaje);
      } catch (MailException excepcion) {
        LOGGER.warning("No se pudo enviar un correo. Se continuará con los demás destinatarios.");
      }
    }
  }
}
