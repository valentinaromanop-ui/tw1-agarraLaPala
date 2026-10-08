package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.*;

import com.tallerwebi.dominio.*;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
public class RepositorioDetalleVacanteTest {

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  @Transactional
  @Rollback
  public void deberiaConservarLaDescripcionYLaRelacionConLaPostulacion() {
    RepositorioVacante repositorio = new RepositorioVacanteImpl(sessionFactory);
    Vacante vacante = new Vacante();
    vacante.setFuente("JOBICY");
    vacante.setIdExterno(123L);
    vacante.setDescripcion("Descripción larga. ".repeat(100));
    repositorio.guardar(vacante);
    Usuario usuario = new Usuario();
    usuario.setEmail("detalle@test.com");
    usuario.setPassword("123");
    usuario.setRol("CANDIDATO");
    sessionFactory.getCurrentSession().persist(usuario);
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(usuario);
    postulacion.setVacante(vacante);
    postulacion.setOfertaId("JOBICY:123");
    postulacion.setFechaPostulacion(LocalDateTime.now());
    postulacion.setEstado("Registrada");
    new RepositorioPostulacionImpl(sessionFactory).guardar(postulacion);
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();

    Postulacion obtenida = new RepositorioPostulacionImpl(sessionFactory)
      .buscarPorUsuarioYOferta(usuario.getId(), "JOBICY:123");

    assertEquals(vacante.getDescripcion(), obtenida.getVacante().getDescripcion());
    assertEquals(vacante.getId(), repositorio.buscarPorFuenteEIdExterno("JOBICY", 123L).getId());
  }

  @Test
  @Transactional
  @Rollback
  public void noDeberiaMostrarCopiasExternasComoVacantesLocales() {
    Skill skill = new Skill();
    skill.setNombre("java");
    sessionFactory.getCurrentSession().persist(skill);
    Vacante externa = new Vacante();
    externa.setFuente("JOBICY");
    externa.setIdExterno(50L);
    VacanteSkill relacion = new VacanteSkill();
    relacion.setVacante(externa);
    relacion.setSkill(skill);
    externa.getVacanteSkills().add(relacion);
    RepositorioVacante repositorio = new RepositorioVacanteImpl(sessionFactory);
    repositorio.guardar(externa);

    assertTrue(repositorio.buscarPorSkills(List.of("java")).isEmpty());
  }
}
