package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

public class ServicioNewsletterTest {

  private RepositorioNewsletter repositorio;
  private ApplicationEventPublisher eventos;
  private ServicioNewsletterImpl servicio;
  private SpringTemplateEngine templateEngine;

  @BeforeEach
  public void init() {
    repositorio = mock(RepositorioNewsletter.class);
    eventos = mock(ApplicationEventPublisher.class);
    templateEngine = mock(SpringTemplateEngine.class);
    when(templateEngine.process(eq("correos/bienvenida-newsletter"), any(Context.class)))
      .thenReturn("<h1>Bienvenida</h1>");
    servicio =
      new ServicioNewsletterImpl(
        repositorio,
        eventos,
        templateEngine,
        "http://localhost:8080/spring"
      );
  }

  @Test
  public void correoNuevoSeGuardaYRecibeBienvenida() {
    List<Object> enviados = new ArrayList<>();
    servicio =
      new ServicioNewsletterImpl(
        repositorio,
        enviados::add,
        templateEngine,
        "http://localhost:8080/spring"
      );

    boolean nueva = servicio.suscribir("  Persona@correo.com  ");

    assertThat(nueva, is(true));
    verify(repositorio).buscarPorEmail("persona@correo.com");
    verify(repositorio).guardar(any(SuscriptorNewsletter.class));
    Correo correo = (Correo) enviados.getFirst();
    assertThat(correo.getDestinatarios(), containsInAnyOrder("persona@correo.com"));
    assertThat(correo.isHtml(), is(true));
    assertThat(correo.getMensaje(), equalTo("<h1>Bienvenida</h1>"));
  }

  @Test
  public void correoRepetidoNoSeGuardaNiRecibeOtroMail() {
    when(repositorio.buscarPorEmail("persona@correo.com")).thenReturn(new SuscriptorNewsletter());

    boolean nueva = servicio.suscribir("persona@correo.com");

    assertThat(nueva, is(false));
    verify(repositorio, never()).guardar(any());
    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  public void correoInvalidoNoSeGuarda() {
    assertThrows(IllegalArgumentException.class, () -> servicio.suscribir("sin-arroba"));

    verify(repositorio, never()).guardar(any());
    verify(eventos, never()).publishEvent(any(Object.class));
  }
}
