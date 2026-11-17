package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class CreadorAdmin implements ApplicationListener<ContextRefreshedEvent> {

  private final ServicioLogin servicioLogin;

  @Autowired
  public CreadorAdmin(ServicioLogin servicioLogin) {
    this.servicioLogin = servicioLogin;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent evento) {
    Usuario admin = new Usuario();
    admin.setEmail("admin@agarralapala.com");
    admin.setPassword("Admin1234");
    admin.setRol("ADMIN");
    admin.setNombre("Admin");
    try {
      servicioLogin.registrar(admin);
    } catch (UsuarioExistente ignored) {
      // el admin ya existe, no hay nada que hacer
    }
  }
}
