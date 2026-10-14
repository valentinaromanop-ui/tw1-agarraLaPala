package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPagoImplTest {

  private RepositorioUsuario repositorioUsuario;

  private RepositorioPago repositorioPago;

  private RepositorioSuscripcion repositorioSuscripcion;

  private Usuario usuario;
  private ServicioPagoImpl servicio;

  @BeforeEach
  void init() {
    repositorioUsuario = org.mockito.Mockito.mock(RepositorioUsuario.class);
    repositorioPago = org.mockito.Mockito.mock(RepositorioPago.class);
    repositorioSuscripcion = org.mockito.Mockito.mock(RepositorioSuscripcion.class);
    usuario = new Usuario();
    usuario.setId(1L);
    usuario.setRol("POSTULANTE");
    servicio = new ServicioPagoImpl(repositorioUsuario, repositorioPago, repositorioSuscripcion);
    when(repositorioUsuario.buscarPorId(1L)).thenReturn(usuario);
    when(repositorioPago.guardar(any(Pago.class)))
      .thenAnswer(invocacion -> invocacion.getArgument(0));
    when(repositorioSuscripcion.guardar(any(Suscripcion.class)))
      .thenAnswer(invocacion -> invocacion.getArgument(0));
  }

  @Test
  void apruebaPagoValido() {
    Suscripcion suscripcion = servicio.pagar(1L, "Titular", "4111111111111111", "12/99", "123");

    assertEquals(true, suscripcion.getFechaFin().isAfter(LocalDateTime.now().plusDays(29)));
    org.mockito.ArgumentCaptor<Pago> captor = org.mockito.ArgumentCaptor.forClass(Pago.class);
    verify(repositorioPago).guardar(captor.capture());
    assertEquals(EstadoPago.APROBADO, captor.getValue().getEstado());
    assertEquals("1111", captor.getValue().getUltimosCuatro());
  }

  @Test
  void rechazaTarjetaDePrueba() {
    assertThrows(
      IllegalArgumentException.class,
      () -> servicio.pagar(1L, "Titular", "4000000000000002", "12/99", "123")
    );
    org.mockito.ArgumentCaptor<Pago> captor = org.mockito.ArgumentCaptor.forClass(Pago.class);
    verify(repositorioPago).guardar(captor.capture());
    assertEquals(EstadoPago.RECHAZADO, captor.getValue().getEstado());
    org.mockito.Mockito
      .verify(repositorioSuscripcion, org.mockito.Mockito.never())
      .guardar(any(Suscripcion.class));
  }

  @Test
  void extiendeSuscripcionActiva() {
    Suscripcion anterior = new Suscripcion();
    anterior.setUsuario(usuario);
    anterior.setFechaFin(LocalDateTime.now().plusDays(5));
    LocalDateTime fechaEsperada = anterior.getFechaFin().plusDays(30);
    when(repositorioSuscripcion.buscarPorUsuarioId(1L)).thenReturn(anterior);

    Suscripcion actual = servicio.pagar(1L, "Titular", "4111111111111111", "12/99", "123");

    assertEquals(fechaEsperada, actual.getFechaFin());
  }
}
