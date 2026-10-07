package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

public class ControladorLoginTest {

  private ControladorLogin controladorLogin;
  private DatosLogin datosLoginMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private ServicioLogin servicioLoginMock;
  private MockHttpServletRequest registrationRequest;

  @BeforeEach
  public void init() {
    datosLoginMock = new DatosLogin("dami@unlam.com", "123");
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    servicioLoginMock = mock(ServicioLogin.class);
    controladorLogin = new ControladorLogin(servicioLoginMock);
    registrationRequest = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(registrationRequest));
  }

  @AfterEach
  public void limpiarRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  public void loginConUsuarioYPasswordInorrectosDeberiaLlevarALoginNuevamente() {
    // preparacion
    when(servicioLoginMock.consultarUsuario(anyString(), anyString())).thenReturn(null);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("login"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Usuario o clave incorrecta")
    );
    assertThat(datosLoginMock.getPassword(), org.hamcrest.Matchers.nullValue());
    verify(sessionMock, times(0)).setAttribute("ROL", "ADMIN");
  }

  @Test
  public void loginConUsuarioYPasswordCorrectosDeberiaLlevarAVacantesRecomendadas() {
    // preparacion
    Usuario usuarioEncontradoMock = mock(Usuario.class);
    when(usuarioEncontradoMock.getRol()).thenReturn("ADMIN");

    when(requestMock.getSession()).thenReturn(sessionMock);
    when(servicioLoginMock.consultarUsuario(anyString(), anyString()))
      .thenReturn(usuarioEncontradoMock);

    // ejecucion
    ModelAndView modelAndView = controladorLogin.validarLogin(datosLoginMock, requestMock);

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/vacantes/recomendadas"));
    verify(sessionMock, times(1)).setAttribute("ROL", usuarioEncontradoMock.getRol());
  }

  @Test
  public void irALoginDeberiaRetornarFormularioUnico() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irALogin();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("login"));
    assertThat(modelAndView.getModel().get("datosLogin"), instanceOf(DatosLogin.class));
  }

  @Test
  public void irARegistroDeberiaRetornarSelectorDeTipoDeCuenta() {
    ModelAndView modelAndView = controladorLogin.irARegistro();

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro"));
  }

  @Test
  public void irARegistroEmpleadorDeberiaRetornarFormularioConDatosVacios() {
    ModelAndView modelAndView = controladorLogin.irARegistroEmpleador();

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-empleador"));
    assertThat(
      modelAndView.getModel().get("datosRegistroEmpleador"),
      instanceOf(DatosRegistroEmpleador.class)
    );
  }

  @Test
  public void irARegistroCandidatoDeberiaRetornarFormularioYHabilidades() {
    ModelAndView modelAndView = controladorLogin.irARegistroCandidato();

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-candidato"));
    assertThat(
      modelAndView.getModel().get("datosRegistroCandidato"),
      instanceOf(DatosRegistroCandidato.class)
    );
    assertThat(
      modelAndView.getModel().get("habilidadesDisponibles"),
      instanceOf(java.util.List.class)
    );
  }

  @Test
  public void registroEmpleadorConDatosValidosDeberiaRedirigirAlLogin() throws Exception {
    DatosRegistroEmpleador datos = new DatosRegistroEmpleador();
    datos.setEmpresa("Empresa SA");
    datos.setLegajo("12345");
    datos.setEmail("contacto@empresa.com");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("secreto");

    ModelAndView modelAndView = controladorLogin.registrarEmpleador(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/vacantes/recomendadas"));
    verify(servicioLoginMock)
      .registrar(
        argThat(usuario ->
          usuario.getEmail().equals("contacto@empresa.com") &&
          usuario.getPassword().equals("secreto") &&
          usuario.getRol().equals("EMPLEADOR") &&
          usuario.getEmpresa().equals("Empresa SA") &&
          usuario.getLegajo().equals("12345")
        )
      );
    assertThat(
      registrationRequest.getSession().getAttribute("ROL").toString(),
      equalToIgnoringCase("EMPLEADOR")
    );
  }

  @Test
  public void registroEmpleadorConCorreoInvalidoDeberiaMostrarError() {
    DatosRegistroEmpleador datos = new DatosRegistroEmpleador();
    datos.setEmpresa("Empresa SA");
    datos.setLegajo("12345");
    datos.setEmail("correo-invalido");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("secreto");

    ModelAndView modelAndView = controladorLogin.registrarEmpleador(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-empleador"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Ingresá un correo electrónico válido.")
    );
    assertThat(datos.getPassword(), org.hamcrest.Matchers.nullValue());
    assertThat(datos.getConfirmarPassword(), org.hamcrest.Matchers.nullValue());
  }

  @Test
  public void registroEmpleadorConCorreoExistenteDeberiaMostrarError() throws Exception {
    DatosRegistroEmpleador datos = new DatosRegistroEmpleador();
    datos.setEmpresa("Empresa SA");
    datos.setLegajo("12345");
    datos.setEmail("contacto@empresa.com");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("secreto");
    doThrow(new UsuarioExistente()).when(servicioLoginMock).registrar(any(Usuario.class));

    ModelAndView modelAndView = controladorLogin.registrarEmpleador(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-empleador"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Ya existe una cuenta registrada con ese correo.")
    );
  }

  @Test
  public void registroEmpleadorConContrasenasDistintasDeberiaMostrarError() {
    DatosRegistroEmpleador datos = new DatosRegistroEmpleador();
    datos.setEmpresa("Empresa SA");
    datos.setLegajo("12345");
    datos.setEmail("contacto@empresa.com");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("otro-secreto");

    ModelAndView modelAndView = controladorLogin.registrarEmpleador(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-empleador"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Las contraseñas no coinciden.")
    );
  }

  @Test
  public void registroCandidatoConDatosValidosDeberiaRedirigirAlLogin() throws Exception {
    DatosRegistroCandidato datos = new DatosRegistroCandidato();
    datos.setEmail("persona@example.com");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("secreto");
    datos.setHabilidades(java.util.List.of("Java", "SQL"));

    ModelAndView modelAndView = controladorLogin.registrarCandidato(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/vacantes/recomendadas"));
    verify(servicioLoginMock)
      .registrar(
        argThat(usuario ->
          usuario.getRol().equals("CANDIDATO") &&
          usuario.getHabilidades().equals(java.util.List.of("Java", "SQL"))
        )
      );
    assertThat(
      registrationRequest.getSession().getAttribute("ROL").toString(),
      equalToIgnoringCase("CANDIDATO")
    );
  }

  @Test
  public void registroCandidatoConCamposVaciosDeberiaMostrarError() {
    ModelAndView modelAndView = controladorLogin.registrarCandidato(new DatosRegistroCandidato());

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-candidato"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Completá todos los campos.")
    );
  }

  @Test
  public void registroCandidatoConCorreoExistenteDeberiaMostrarError() throws Exception {
    DatosRegistroCandidato datos = new DatosRegistroCandidato();
    datos.setEmail("persona@example.com");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("secreto");
    doThrow(new UsuarioExistente()).when(servicioLoginMock).registrar(any(Usuario.class));

    ModelAndView modelAndView = controladorLogin.registrarCandidato(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-candidato"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Ya existe una cuenta registrada con ese correo.")
    );
    assertThat(
      modelAndView.getModel().get("habilidadesDisponibles"),
      instanceOf(java.util.List.class)
    );
  }

  @Test
  public void registroCandidatoConContrasenasDistintasDeberiaMostrarError() {
    DatosRegistroCandidato datos = new DatosRegistroCandidato();
    datos.setEmail("persona@example.com");
    datos.setPassword("secreto");
    datos.setConfirmarPassword("otro-secreto");

    ModelAndView modelAndView = controladorLogin.registrarCandidato(datos);

    assertThat(modelAndView.getViewName(), equalToIgnoringCase("registro-candidato"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Las contraseñas no coinciden.")
    );
  }

  @Test
  public void irAHomeDeberiaRetornarVistaHome() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.irAHome();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("home"));
  }

  @Test
  public void inicioDeberiaRedirigirALogin() {
    // ejecucion
    ModelAndView modelAndView = controladorLogin.inicio();

    // validacion
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("redirect:/login"));
  }
}
