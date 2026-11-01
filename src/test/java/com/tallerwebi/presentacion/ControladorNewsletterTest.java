package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioNewsletter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorNewsletterTest {

  private ServicioNewsletter servicio;
  private ControladorNewsletter controlador;
  private DatosNewsletter datos;

  @BeforeEach
  public void init() {
    servicio = mock(ServicioNewsletter.class);
    controlador = new ControladorNewsletter(servicio);
    datos = new DatosNewsletter();
    datos.setEmail("persona@example.com");
  }

  @Test
  public void suscripcionNuevaDevuelveConfirmacionEnElFragmento() {
    when(servicio.suscribir(datos.getEmail())).thenReturn(true);

    ModelAndView vista = controlador.suscribir(datos);

    assertThat(vista.getViewName(), equalTo("fragments/footer :: resultadoNewsletter"));
    assertThat(
      vista.getModel().get("mensajeNewsletter"),
      equalTo("¡Gracias! Tu suscripción al newsletter quedó registrada.")
    );
    verify(servicio).suscribir(datos.getEmail());
  }

  @Test
  public void suscripcionExistenteMuestraAviso() {
    when(servicio.suscribir(datos.getEmail())).thenReturn(false);

    ModelAndView vista = controlador.suscribir(datos);

    assertThat(vista.getViewName(), equalTo("fragments/footer :: resultadoNewsletter"));
    assertThat(
      vista.getModel().get("mensajeNewsletter"),
      equalTo("Este correo ya está suscrito al newsletter.")
    );
  }

  @Test
  public void correoInvalidoDevuelveElFragmentoConError() {
    when(servicio.suscribir(datos.getEmail()))
      .thenThrow(new IllegalArgumentException("Correo inválido"));

    ModelAndView vista = controlador.suscribir(datos);

    assertThat(vista.getViewName(), equalTo("fragments/footer :: resultadoNewsletter"));
    assertThat(vista.getModel().get("errorNewsletter"), equalTo("Correo inválido"));
  }
}
