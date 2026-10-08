package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ServicioLoginTest {

  private ServicioLogin servicioLogin;
  private RepositorioUsuario repositorioUsuarioMock;

  @BeforeEach
  public void init() {
    this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
    this.servicioLogin = new ServicioLoginImpl(this.repositorioUsuarioMock);
  }

  @Test
  public void consultarUsuarioDeberiaLlamarAlRepositorio() {
    String email = "test@test.com";
    String password = "password";
    Usuario usuarioEsperado = new Usuario();
    usuarioEsperado.setPassword(new BCryptPasswordEncoder().encode(password));
    when(this.repositorioUsuarioMock.buscar(email)).thenReturn(usuarioEsperado);

    Usuario usuarioObtenido = this.servicioLogin.consultarUsuario(email, password);

    assertThat(usuarioObtenido, equalTo(usuarioEsperado));
    verify(this.repositorioUsuarioMock, times(1)).buscar(email);
  }

  @Test
  public void registrarUsuarioSiNoExisteDeberiaGuardarlo() throws UsuarioExistente {
    // preparacion
    Usuario usuario = new Usuario();
    usuario.setEmail("nuevo@test.com");
    usuario.setPassword("123");
    when(this.repositorioUsuarioMock.buscar(usuario.getEmail())).thenReturn(null);

    this.servicioLogin.registrar(usuario);

    assertThat(usuario.getPassword().startsWith("$2a$"), equalTo(true));
    assertThat(new BCryptPasswordEncoder().matches("123", usuario.getPassword()), equalTo(true));
    verify(this.repositorioUsuarioMock, times(1)).guardar(usuario);
  }

  @Test
  public void registrarUsuarioSiExisteDeberiaLanzarExcepcion() {
    // preparacion
    Usuario usuario = new Usuario();
    usuario.setEmail("existe@test.com");
    usuario.setPassword("123");
    when(this.repositorioUsuarioMock.buscar(usuario.getEmail())).thenReturn(new Usuario());

    assertThrows(UsuarioExistente.class, () -> this.servicioLogin.registrar(usuario));
    verify(this.repositorioUsuarioMock, times(0)).guardar(usuario);
  }

  @Test
  public void consultarUsuarioConContrasenaIncorrectaDeberiaDevolverNull() {
    Usuario usuario = new Usuario();
    usuario.setPassword(new BCryptPasswordEncoder().encode("correcta"));
    when(this.repositorioUsuarioMock.buscar("test@test.com")).thenReturn(usuario);

    assertThat(this.servicioLogin.consultarUsuario("test@test.com", "incorrecta"), equalTo(null));
  }
}
