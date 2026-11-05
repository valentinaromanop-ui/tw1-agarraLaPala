package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.config.GoogleCalendarConfig;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import com.tallerwebi.infraestructura.CifradorTokensGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ServicioConexionGoogleTest {

  private RepositorioUsuario repositorioUsuarioMock;
  private RepositorioConexionGoogle repositorioConexionGoogleMock;
  private ClienteGoogleCalendar cliente;
  private ServicioConexionGoogle servicioConexionGoogle;
  private CifradorTokensGoogle cifrador;
  private Usuario empleador;

  @BeforeEach
  public void init() {
    repositorioUsuarioMock = mock(RepositorioUsuario.class);
    repositorioConexionGoogleMock = mock(RepositorioConexionGoogle.class);
    cliente = mock(ClienteGoogleCalendar.class);
    GoogleCalendarConfig googleCalendarConfigMock = mock(GoogleCalendarConfig.class);
    when(googleCalendarConfigMock.estaDisponible()).thenReturn(true);
    cifrador = mock(CifradorTokensGoogle.class);
    servicioConexionGoogle =
      new ServicioConexionGoogleImpl(
        repositorioUsuarioMock,
        repositorioConexionGoogleMock,
        cliente,
        cifrador,
        googleCalendarConfigMock
      );
    empleador = new Usuario();
    empleador.setId(7L);
    empleador.setRol("EMPLEADOR");
    when(repositorioUsuarioMock.buscarPorId(7L)).thenReturn(empleador);
  }

  @Test
  public void guardaSoloElRefreshTokenCifrado() {
    // preparacion
    when(cliente.intercambiarCodigo("codigo")).thenReturn("renovacion");
    when(cifrador.cifrar("renovacion", 7L)).thenReturn("token-cifrado");

    // ejecucion
    servicioConexionGoogle.conectar(7L, "codigo");

    // validacion
    ArgumentCaptor<ConexionGoogleCalendar> captor = ArgumentCaptor.forClass(
      ConexionGoogleCalendar.class
    );
    verify(repositorioConexionGoogleMock).guardar(captor.capture());
    assertThat(captor.getValue().getRefreshTokenCifrado(), equalTo("token-cifrado"));
    verify(repositorioUsuarioMock).buscarPorIdParaActualizar(7L);
  }

  @Test
  public void reconectarActualizaLaConexionExistente() {
    // preparacion
    ConexionGoogleCalendar conexion = new ConexionGoogleCalendar(empleador, "anterior-cifrado");
    when(repositorioConexionGoogleMock.buscar(7L)).thenReturn(conexion);
    when(cliente.intercambiarCodigo("codigo")).thenReturn("nuevo");
    when(cifrador.cifrar("nuevo", 7L)).thenReturn("nuevo-cifrado");

    // ejecucion
    servicioConexionGoogle.conectar(7L, "codigo");

    // validacion
    assertThat(conexion.getRefreshTokenCifrado(), equalTo("nuevo-cifrado"));
    verify(repositorioConexionGoogleMock, never()).guardar(any());
  }

  @Test
  public void falloDeGoogleConservaLaConexionAnterior() {
    // preparacion
    ConexionGoogleCalendar conexion = new ConexionGoogleCalendar(empleador, "anterior-cifrado");
    when(repositorioConexionGoogleMock.buscar(7L)).thenReturn(conexion);
    when(cliente.intercambiarCodigo("codigo")).thenThrow(new ConexionGoogleException());

    // ejecucion
    assertThrows(
      ConexionGoogleException.class,
      () -> servicioConexionGoogle.conectar(7L, "codigo")
    );

    // validacion
    assertThat(conexion.getRefreshTokenCifrado(), equalTo("anterior-cifrado"));
    verify(repositorioConexionGoogleMock, never()).guardar(any());
  }

  @Test
  public void obtieneAccesoRenovandoDesdeElTokenPersistido() {
    // preparacion
    when(repositorioConexionGoogleMock.buscar(7L))
      .thenReturn(new ConexionGoogleCalendar(empleador, "renovacion-cifrada"));
    when(cifrador.descifrar("renovacion-cifrada", 7L)).thenReturn("renovacion");
    when(cliente.renovarAcceso("renovacion")).thenReturn("acceso");

    // ejecucion
    String tokenObtenido = servicioConexionGoogle.obtenerTokenAcceso(7L);

    // validacion
    assertThat(tokenObtenido, equalTo("acceso"));
  }

  @Test
  public void rechazaUsuariosQueNoSonEmpleadores() {
    // preparacion
    empleador.setRol("CANDIDATO");

    // ejecucion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioConexionGoogle.conectar(7L, "codigo")
    );

    // validacion
    verifyNoInteractions(cliente, repositorioConexionGoogleMock);
  }

  @Test
  public void renovarAccesoDeberiaRechazarUsuariosQueNoSonEmpleadores() {
    // preparacion
    empleador.setRol("CANDIDATO");

    // ejecucion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioConexionGoogle.obtenerTokenAcceso(7L)
    );

    // validacion
    verifyNoInteractions(cliente, repositorioConexionGoogleMock);
  }

  @Test
  public void requiereUnaConexionParaRenovar() {
    // preparacion
    when(repositorioConexionGoogleMock.buscar(7L)).thenReturn(null);

    // ejecucion
    assertThrows(
      ConexionGoogleException.class,
      () -> servicioConexionGoogle.obtenerTokenAcceso(7L)
    );

    // validacion
    verifyNoInteractions(cliente);
  }
}
