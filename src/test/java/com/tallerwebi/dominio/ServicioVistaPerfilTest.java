package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ServicioVistaPerfilTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioVistaPerfil repositorioVista;
  private ServicioVistaPerfilImpl servicio;
  private Usuario postulante;
  private Usuario empleador;

  @BeforeEach
  void iniciar() {
    repositorioUsuario = Mockito.mock(RepositorioUsuario.class);
    repositorioVista = Mockito.mock(RepositorioVistaPerfil.class);
    servicio = new ServicioVistaPerfilImpl(repositorioUsuario, repositorioVista);
    postulante = usuario(1L, "CANDIDATO");
    empleador = usuario(2L, "EMPLEADOR");
    when(repositorioUsuario.buscarPorId(1L)).thenReturn(postulante);
    when(repositorioUsuario.buscarPorId(2L)).thenReturn(empleador);
  }

  @Test
  void registraUnaVista() {
    servicio.registrarVista(1L, 2L, "Empresa", 10L, "Java", TipoVistaPerfil.PERFIL);

    verify(repositorioVista).guardar(any(VistaPerfil.class));
  }

  @Test
  void actualizaLaVistaDentroDeLas24Horas() {
    VistaPerfil existente = new VistaPerfil();
    existente.setFecha(LocalDateTime.now().minusHours(1));
    when(
      repositorioVista.buscarReciente(
        Mockito.eq(1L),
        Mockito.eq(2L),
        Mockito.eq(10L),
        Mockito.eq(TipoVistaPerfil.CV),
        any(LocalDateTime.class)
      )
    )
      .thenReturn(existente);

    servicio.registrarVista(1L, 2L, "Empresa", 10L, "Java", TipoVistaPerfil.CV);

    verify(repositorioVista, never()).guardar(any(VistaPerfil.class));
    assertTrue(existente.getFecha().isBefore(LocalDateTime.now()));
  }

  @Test
  void descartaAQuienNoEsEmpleador() {
    empleador.setRol("CANDIDATO");

    servicio.registrarVista(1L, 2L, "Empresa", null, null, TipoVistaPerfil.PERFIL);

    verify(repositorioVista, never()).guardar(any(VistaPerfil.class));
  }

  private Usuario usuario(Long id, String rol) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setRol(rol);
    return usuario;
  }
}
