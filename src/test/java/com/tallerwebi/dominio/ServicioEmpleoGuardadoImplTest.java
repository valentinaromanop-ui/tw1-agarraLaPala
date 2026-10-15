package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioEmpleoGuardadoImplTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioEmpleoGuardado repositorioEmpleoGuardado;
  private ServicioEmpleoGuardadoImpl servicio;
  private Usuario usuario;

  @BeforeEach
  void iniciar() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioEmpleoGuardado = mock(RepositorioEmpleoGuardado.class);
    servicio = new ServicioEmpleoGuardadoImpl(repositorioUsuario, repositorioEmpleoGuardado);
    usuario = new Usuario();
    usuario.setId(1L);
    usuario.setRol("CANDIDATO");
    when(repositorioUsuario.buscarPorId(1L)).thenReturn(usuario);
  }

  @Test
  void guardaOfertaYCompletaDatosObligatorios() {
    EmpleoGuardado datos = new EmpleoGuardado();
    datos.setIdOferta(" LOCAL:42 ");
    datos.setTitulo(" ");
    when(repositorioEmpleoGuardado.guardar(any(EmpleoGuardado.class)))
      .thenAnswer(invocacion -> invocacion.getArgument(0));

    EmpleoGuardado resultado = servicio.guardar(1L, datos);

    assertSame(datos, resultado);
    assertEquals("LOCAL:42", resultado.getIdOferta());
    assertEquals("Oferta sin título", resultado.getTitulo());
    assertEquals(usuario, resultado.getUsuario());
    verify(repositorioEmpleoGuardado).guardar(datos);
  }

  @Test
  void evitaCrearDuplicadosYDevuelveElExistente() {
    EmpleoGuardado existente = new EmpleoGuardado();
    existente.setIdOferta("LOCAL:42");
    EmpleoGuardado nuevo = new EmpleoGuardado();
    nuevo.setIdOferta(" LOCAL:42 ");
    when(repositorioEmpleoGuardado.buscarPorUsuarioYOferta(1L, "LOCAL:42")).thenReturn(existente);

    EmpleoGuardado resultado = servicio.guardar(1L, nuevo);

    assertSame(existente, resultado);
    verify(repositorioEmpleoGuardado, never()).guardar(any(EmpleoGuardado.class));
  }

  @Test
  void quitaOfertaExistente() {
    EmpleoGuardado existente = new EmpleoGuardado();
    existente.setIdOferta("LOCAL:42");
    when(repositorioEmpleoGuardado.buscarPorUsuarioYOferta(1L, "LOCAL:42")).thenReturn(existente);

    servicio.quitar(1L, " LOCAL:42 ");

    verify(repositorioEmpleoGuardado).eliminar(existente);
  }

  @Test
  void quitarOfertaInexistenteEsIdempotente() {
    servicio.quitar(1L, "LOCAL:42");

    verify(repositorioEmpleoGuardado, never()).eliminar(any(EmpleoGuardado.class));
  }

  @Test
  void listaOfertasYIdsGuardados() {
    EmpleoGuardado primero = new EmpleoGuardado();
    primero.setIdOferta("LOCAL:1");
    EmpleoGuardado segundo = new EmpleoGuardado();
    segundo.setIdOferta("JOBICY:2");
    when(repositorioEmpleoGuardado.buscarPorUsuarioId(1L)).thenReturn(List.of(primero, segundo));

    assertEquals(List.of(primero, segundo), servicio.listar(1L));
    assertEquals(List.of("LOCAL:1", "JOBICY:2"), servicio.listarIds(1L));
  }

  @Test
  void rechazaUsuarioNoPostulanteYDatosInvalidos() {
    Usuario empleador = new Usuario();
    empleador.setRol("EMPLEADOR");
    when(repositorioUsuario.buscarPorId(2L)).thenReturn(empleador);

    assertThrows(IllegalArgumentException.class, () -> servicio.guardar(2L, new EmpleoGuardado()));
    assertThrows(IllegalArgumentException.class, () -> servicio.guardar(1L, null));
    assertThrows(IllegalArgumentException.class, () -> servicio.quitar(1L, " "));
  }
}
