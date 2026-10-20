package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.PerfilPostulante;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.PostulanteSkill;
import com.tallerwebi.dominio.RepositorioCurriculumGenerado;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Usuario;
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
class RepositorioPostulacionTest {

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  @Transactional
  @Rollback
  void obtienePostulacionesOrdenadasConUsuarioYHabilidades() {
    Usuario primero = guardarUsuario("primero@test.com", "Ada", "Lovelace");
    primero.setHabilidades(List.of("Java", "SQL"));
    Usuario segundo = guardarUsuario("segundo@test.com", "Grace", "Hopper");
    segundo.setHabilidades(List.of("COBOL"));
    Postulacion antigua = guardarPostulacion(
      primero,
      "LOCAL:55",
      LocalDateTime.of(2025, 1, 1, 8, 0)
    );
    guardarPostulacion(segundo, "55", LocalDateTime.of(2025, 1, 2, 8, 0));
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();

    RepositorioPostulacionImpl repositorio = new RepositorioPostulacionImpl(sessionFactory);
    List<Postulacion> resultado = repositorio.buscarPorVacante(55L);

    assertEquals(2, resultado.size());
    assertEquals("Grace", resultado.get(0).getUsuario().getNombre());
    assertEquals(List.of("COBOL"), resultado.get(0).getUsuario().getHabilidades());
    assertEquals(antigua.getId(), resultado.get(1).getId());
    assertEquals(List.of("Java", "SQL"), resultado.get(1).getUsuario().getHabilidades());
    assertEquals(antigua.getId(), repositorio.buscarPorIdYVacante(antigua.getId(), 55L).getId());
    assertNull(repositorio.buscarPorIdYVacante(antigua.getId(), 56L));
  }

  @Test
  @Transactional
  @Rollback
  void obtieneEnUnaConsultaLosUsuariosConCvOriginal() {
    Usuario conCv = guardarUsuario("cv@test.com", "Nombre", "Apellido");
    CurriculumGenerado curriculum = new CurriculumGenerado();
    curriculum.setUsuario(conCv);
    curriculum.setNombreArchivoOriginal("cv.pdf");
    curriculum.setArchivoOriginal(new byte[] { 1, 2, 3 });
    curriculum.setGeneradoEn(LocalDateTime.now());
    sessionFactory.getCurrentSession().persist(curriculum);
    sessionFactory.getCurrentSession().flush();

    RepositorioCurriculumGenerado repositorio = new RepositorioCurriculumGeneradoImpl(
      sessionFactory
    );

    assertEquals(
      List.of(conCv.getId()),
      repositorio.buscarUsuarioIdsConCvOriginal(List.of(conCv.getId()))
    );
    assertEquals(List.of(), repositorio.buscarUsuarioIdsConCvOriginal(List.of()));
  }

  @Test
  @Transactional
  @Rollback
  void obtieneElPerfilDeLecturaConUsuarioYHabilidadesEnUnaConsulta() {
    Usuario usuario = guardarUsuario("perfil@test.com", "Ada", "Lovelace");
    Skill javaSkill = new Skill();
    javaSkill.setNombre("Java");
    sessionFactory.getCurrentSession().persist(javaSkill);
    PerfilPostulante perfil = new PerfilPostulante();
    perfil.setUsuario(usuario);
    perfil.setPresentacion("Desarrolladora");
    perfil.setNivelExperiencia("Senior");
    perfil.setIdiomas(java.util.Map.of("Inglés", "Avanzado"));
    sessionFactory.getCurrentSession().persist(perfil);
    PostulanteSkill relacion = new PostulanteSkill();
    relacion.setPostulante(perfil);
    relacion.setSkill(javaSkill);
    sessionFactory.getCurrentSession().persist(relacion);
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();

    PerfilPostulante resultado = new RepositorioPerfilPostulanteImpl(sessionFactory)
      .buscarPorUsuarioIdParaLectura(usuario.getId());

    assertEquals("Ada", resultado.getUsuario().getNombre());
    assertEquals("perfil@test.com", resultado.getUsuario().getEmail());
    assertEquals("Desarrolladora", resultado.getPresentacion());
    assertEquals("Senior", resultado.getNivelExperiencia());
    assertEquals("Java", resultado.getPostulanteSkills().get(0).getSkill().getNombre());
    assertEquals("Avanzado", resultado.getIdiomas().get("Inglés"));
  }

  private Usuario guardarUsuario(String email, String nombre, String apellido) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("test");
    usuario.setRol("CANDIDATO");
    usuario.setNombre(nombre);
    usuario.setApellido(apellido);
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }

  private Postulacion guardarPostulacion(Usuario usuario, String oferta, LocalDateTime fecha) {
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(usuario);
    postulacion.setIdOferta(oferta);
    postulacion.setFechaPostulacion(fecha);
    sessionFactory.getCurrentSession().persist(postulacion);
    return postulacion;
  }
}
