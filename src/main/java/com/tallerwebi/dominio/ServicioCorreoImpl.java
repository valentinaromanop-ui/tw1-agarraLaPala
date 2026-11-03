package com.tallerwebi.dominio;

import jakarta.mail.MessagingException;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void enviar(Correo correo) {
    if (!habilitado) {
      LOGGER.info("Envío de correos deshabilitado. Configurá correo.habilitado para activarlo.");
      return;
    }
    for (String destinatario : correo.getDestinatarios()) {
      try {
        enviarADestinatario(correo, destinatario);
      } catch (MailException | MessagingException excepcion) {
        LOGGER.warning("No se pudo enviar un correo. Se continuará con los demás destinatarios.");
      }
    }
  }

  private void enviarADestinatario(Correo correo, String destinatario) throws MessagingException {
    if (correo.isHtml()) {
      MimeMessageHelper mensaje = new MimeMessageHelper(mailSender.createMimeMessage(), "UTF-8");
      mensaje.setFrom(remitente);
      mensaje.setTo(destinatario);
      mensaje.setSubject(correo.getAsunto());
      mensaje.setText(correo.getMensaje(), true);
      mailSender.send(mensaje.getMimeMessage());
    } else {
      SimpleMailMessage mensaje = new SimpleMailMessage();
      mensaje.setFrom(remitente);
      mensaje.setTo(destinatario);
      mensaje.setSubject(correo.getAsunto());
      mensaje.setText(correo.getMensaje());
      mailSender.send(mensaje);
    }
  }
}
