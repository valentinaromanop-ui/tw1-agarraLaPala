package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Transactional
public class ServicioNewsletterImpl implements ServicioNewsletter {

  private static final Pattern FORMATO_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final int LONGITUD_MAXIMA = 250;
  private final RepositorioNewsletter repositorio;
  private final ApplicationEventPublisher eventos;
  private final SpringTemplateEngine templateEngine;
  private final String urlBase;

  @Autowired
  public ServicioNewsletterImpl(
    RepositorioNewsletter repositorio,
    ApplicationEventPublisher eventos,
    SpringTemplateEngine templateEngine,
    @Value("${app.url:http://localhost:8080/spring}") String urlBase
  ) {
    this.repositorio = repositorio;
    this.eventos = eventos;
    this.templateEngine = templateEngine;
    this.urlBase = urlBase;
  }

  @Override
  public boolean suscribir(String email) {
    if (email == null) {
      throw new IllegalArgumentException("Ingresá un correo electrónico válido.");
    }
    String normalizado = email.trim().toLowerCase(Locale.ROOT);
    if (normalizado.length() > LONGITUD_MAXIMA || !FORMATO_CORREO.matcher(normalizado).matches()) {
      throw new IllegalArgumentException("Ingresá un correo electrónico válido.");
    }
    if (repositorio.buscarPorEmail(normalizado) != null) {
      return false;
    }
    SuscriptorNewsletter suscriptor = new SuscriptorNewsletter();
    suscriptor.setEmail(normalizado);
    suscriptor.setFechaAlta(LocalDateTime.now());
    repositorio.guardar(suscriptor);
    eventos.publishEvent(
      new Correo(
        List.of(normalizado),
        "Bienvenido a Agarra la Pala",
        getBienvenidaHtmlContent(),
        true
      )
    );
    return true;
  }

  private String getBienvenidaHtmlContent() {
    Context contexto = new Context();
    contexto.setVariable("urlInicio", urlBase + "/home");
    return templateEngine.process("correos/bienvenida-newsletter", contexto);
  }
}
