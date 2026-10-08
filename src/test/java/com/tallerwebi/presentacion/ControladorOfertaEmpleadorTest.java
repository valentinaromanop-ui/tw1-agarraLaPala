package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Vacante;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

public class ControladorOfertaEmpleadorTest {

  private ControladorOfertaEmpleador controlador;
  private ServicioOfertaEmpleador servicio;
  private Usuario empleador;
  private MockHttpServletRequest request;

  @BeforeEach
  public void iniciar() {
    servicio = mock(ServicioOfertaEmpleador.class);
    controlador = new ControladorOfertaEmpleador(servicio);
    empleador = new Usuario();
    empleador.setId(42L);
    empleador.setRol("EMPLEADOR");
    empleador.setEmpresa("Empresa de prueba");
    request = new MockHttpServletRequest();
    HttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "EMPLEADOR");
    session.setAttribute("USUARIO_ID", 42L);
    request.setSession(session);
    when(servicio.obtenerEmpleador(42L)).thenReturn(empleador);
  }

  @Test
  public void crearOfertaDeberiaGuardarYRedirigirALasBusquedas() {
    DatosOfertaEmpleador datos = new DatosOfertaEmpleador();
    datos.setTitulo("Desarrollador");
    datos.setDescripcion("Desarrollo de servicios.");
    datos.setUbicacion("Buenos Aires");
    datos.setModalidad("remoto");
    datos.setHorario("Lunes a viernes");
    datos.setSalario("1200000");
    datos.setSkills("Java, SQL");

    ModelAndView resultado = controlador.crearOferta(datos, request);

    assertThat(resultado.getViewName(), equalTo("redirect:/busquedas-Empleador?publicada=true"));
    verify(servicio).publicar(any(Vacante.class));
    org.mockito.ArgumentCaptor<Vacante> vacanteCaptor = org.mockito.ArgumentCaptor.forClass(
      Vacante.class
    );
    verify(servicio).publicar(vacanteCaptor.capture());
    Vacante guardada = vacanteCaptor.getValue();
    assertThat(guardada.getEmpresa(), equalTo("Empresa de prueba"));
    assertThat(guardada.getEmpleadorId(), equalTo(42L));
    assertThat(guardada.getSalario(), equalTo(new BigDecimal("1200000")));
    assertThat(guardada.getHorario(), equalTo("Lunes a viernes"));
    assertThat(guardada.getFechaPublicacion() != null, is(true));
    assertThat(guardada.getVacanteSkills().size(), equalTo(2));
  }

  @Test
  public void crearOfertaConErroresDeberiaMostrarMensajesSinGuardar() {
    DatosOfertaEmpleador datos = new DatosOfertaEmpleador();

    ModelAndView resultado = controlador.crearOferta(datos, request);

    assertThat(resultado.getViewName(), equalTo("crear-postulacion"));
    assertThat(((List<?>) resultado.getModel().get("errores")).size() > 0, is(true));
  }

  @Test
  public void mostrarBusquedasDeberiaListarVacantesDelEmpleador() {
    List<Vacante> vacantes = Collections.singletonList(new Vacante());
    when(servicio.listarPorEmpleador(42L)).thenReturn(vacantes);

    ModelAndView resultado = controlador.mostrarBusquedas(request);

    assertThat(resultado.getViewName(), equalTo("busquedas-Empleador"));
    assertThat(resultado.getModel().get("vacantes"), equalTo(vacantes));
    verify(servicio).listarPorEmpleador(42L);
  }

  @Test
  public void mostrarBusquedasSinVacantesDeberiaMostrarListaVacia() {
    when(servicio.listarPorEmpleador(42L)).thenReturn(Collections.emptyList());

    ModelAndView resultado = controlador.mostrarBusquedas(request);

    assertThat(resultado.getViewName(), equalTo("busquedas-Empleador"));
    assertThat(((List<?>) resultado.getModel().get("vacantes")).isEmpty(), is(true));
  }

  @Test
  public void mostrarBusquedasSinSesionDeberiaRedirigirAlLogin() {
    request = new MockHttpServletRequest();

    ModelAndView resultado = controlador.mostrarBusquedas(request);

    assertThat(resultado.getViewName(), equalTo("redirect:/login"));
  }
}
