package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service("servicioLogin")
@Transactional
public class ServicioLoginImpl implements ServicioLogin {

  private final RepositorioUsuario repositorioUsuario;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Autowired
  public ServicioLoginImpl(RepositorioUsuario repositorioUsuario) {
    this.repositorioUsuario = repositorioUsuario;
  }

  @Override
  public Usuario consultarUsuario(String email, String password) {
    if (email == null || password == null) {
      return null;
    }
    Usuario usuario = repositorioUsuario.buscar(normalizarEmail(email));
    if (usuario == null || !passwordEncoder.matches(password, usuario.getPassword())) {
      return null;
    }
    return usuario;
  }

  @Override
  public void registrar(Usuario usuario) throws UsuarioExistente {
    usuario.setEmail(normalizarEmail(usuario.getEmail()));
    if (repositorioUsuario.buscar(usuario.getEmail()) != null) {
      throw new UsuarioExistente();
    }
    usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
    repositorioUsuario.guardar(usuario);
  }

  private String normalizarEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
