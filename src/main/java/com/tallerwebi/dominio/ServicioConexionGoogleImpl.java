package com.tallerwebi.dominio;

import com.tallerwebi.config.GoogleCalendarConfig;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import com.tallerwebi.infraestructura.CifradorTokensGoogle;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("servicioConexionGoogle")
@Transactional
public class ServicioConexionGoogleImpl implements ServicioConexionGoogle {

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioConexionGoogle repositorioConexionGoogle;
  private final ClienteGoogleCalendar cliente;
  private final CifradorTokensGoogle cifrador;
  private final GoogleCalendarConfig configuracion;

  @Autowired
  public ServicioConexionGoogleImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioConexionGoogle repositorioConexionGoogle,
    ClienteGoogleCalendar cliente,
    CifradorTokensGoogle cifrador,
    GoogleCalendarConfig configuracion
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioConexionGoogle = repositorioConexionGoogle;
    this.cliente = cliente;
    this.cifrador = cifrador;
    this.configuracion = configuracion;
  }

  @Override
  public boolean estaDisponible() {
    return configuracion.estaDisponible();
  }

  @Override
  public boolean conectado(Long empleadorId) {
    validarEmpleador(empleadorId);
    return repositorioConexionGoogle.buscar(empleadorId) != null;
  }

  @Override
  public String iniciar(Long empleadorId, String state) {
    validarEmpleador(empleadorId);
    validarConfiguracion();
    return cliente.urlAutorizacion(state);
  }

  @Override
  public void conectar(Long empleadorId, String codigo) {
    Usuario empleador = validarEmpleador(empleadorId);
    validarConfiguracion();
    String refreshToken = cliente.intercambiarCodigo(codigo);
    String cifrado = cifrador.cifrar(refreshToken, empleadorId);

    // Evita crear dos conexiones si el mismo empleador reconecta simultaneamente.
    repositorioUsuario.buscarPorIdParaActualizar(empleadorId);
    ConexionGoogleCalendar conexion = repositorioConexionGoogle.buscar(empleadorId);
    if (conexion == null) {
      repositorioConexionGoogle.guardar(new ConexionGoogleCalendar(empleador, cifrado));
    } else {
      // Hibernate guarda este cambio al confirmar la transaccion.
      conexion.setRefreshTokenCifrado(cifrado);
    }
  }

  @Override
  public String obtenerTokenAcceso(Long empleadorId) {
    validarEmpleador(empleadorId);
    validarConfiguracion();
    ConexionGoogleCalendar conexion = repositorioConexionGoogle.buscar(empleadorId);
    if (conexion == null) {
      throw new ConexionGoogleException();
    }
    return cliente.renovarAcceso(
      cifrador.descifrar(conexion.getRefreshTokenCifrado(), empleadorId)
    );
  }

  private Usuario validarEmpleador(Long empleadorId) {
    Usuario usuario = empleadorId == null ? null : repositorioUsuario.buscarPorId(empleadorId);
    if (usuario == null || !"EMPLEADOR".equalsIgnoreCase(usuario.getRol())) {
      throw new IllegalArgumentException("Iniciá sesión como empleador.");
    }
    return usuario;
  }

  private void validarConfiguracion() {
    if (!estaDisponible()) {
      throw new ConexionGoogleException();
    }
  }
}
