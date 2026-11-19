package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

public class ServicioOfertaEmpleadorFiltroTest {

  private RepositorioVacante repositorioVacante;
  private ServicioReporte servicioReporte;
  private ServicioOfertaEmpleadorImpl servicio;

  @BeforeEach
  public void init() {
    repositorioVacante = mock(RepositorioVacante.class);
    servicioReporte = mock(ServicioReporte.class);
    servicio =
      new ServicioOfertaEmpleadorImpl(
        mock(RepositorioUsuario.class),
        repositorioVacante,
        mock(RepositorioPostulacion.class),
        mock(ServicioPerfil.class),
        mock(ServicioPago.class),
        mock(ServicioNotificacionOferta.class),
        servicioReporte,
        mock(ApplicationEventPublisher.class),
        "http://localhost:8080"
      );
  }

  @Test
  public void ofertaConPalabraProhibidaDeberiaOcultarseYReportarseAutomaticamente() {
    Vacante vacante = new Vacante();
    vacante.setTitulo("Buscamos idiota");
    vacante.setDescripcion("Trabajo");
    vacante.setActiva(true);

    servicio.publicar(vacante);

    verify(repositorioVacante).guardar(vacante);
    assertFalse(vacante.getActiva());
    verify(servicioReporte).registrarAutomatico(eq(TipoReporte.OFERTA), any(), any(), any(), any());
  }

  @Test
  public void ofertaLimpiaNoDeberiaGenerarReporte() {
    Vacante vacante = new Vacante();
    vacante.setTitulo("Desarrollador Java");
    vacante.setDescripcion("Buen ambiente");
    vacante.setActiva(true);

    servicio.publicar(vacante);

    assertTrue(vacante.getActiva());
    verify(servicioReporte, never()).registrarAutomatico(any(), any(), any(), any(), any());
  }
}
