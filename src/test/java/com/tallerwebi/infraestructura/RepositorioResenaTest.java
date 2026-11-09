package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tallerwebi.dominio.ReporteResena;
import com.tallerwebi.dominio.Resena;
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
class RepositorioResenaTest {

  @Autowired
  private SessionFactory sessionFactory;

  @Test
  @Transactional
  @Rollback
  void guardaYOrdenaResenasYPermitePersistirReportes() {
    Usuario empleador = guardarUsuario("empleador-resena@test.com", "EMPLEADOR");
    Usuario primeraCandidata = guardarUsuario("candidata-1-resena@test.com", "CANDIDATO");
    Usuario segundaCandidata = guardarUsuario("candidata-2-resena@test.com", "CANDIDATO");
    Resena antigua = guardarResena(
      empleador,
      primeraCandidata,
      3,
      LocalDateTime.of(2026, 1, 1, 10, 0)
    );
    Resena reciente = guardarResena(
      empleador,
      segundaCandidata,
      5,
      LocalDateTime.of(2026, 2, 1, 10, 0)
    );
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();

    RepositorioResenaImpl repositorio = new RepositorioResenaImpl(sessionFactory);
    List<Resena> resultado = repositorio.buscarPorEmpleador(empleador.getId());

    assertEquals(
      List.of(reciente.getId(), antigua.getId()),
      resultado.stream().map(Resena::getId).toList()
    );
    assertEquals("CANDIDATO", resultado.get(0).getCandidato().getRol());
    assertEquals(
      antigua.getId(),
      repositorio.buscarPorCandidatoYEmpleador(primeraCandidata.getId(), empleador.getId()).getId()
    );
    assertNotNull(repositorio.buscarPorIdParaActualizar(reciente.getId()));
    assertEquals(2, repositorio.buscarPorEmpleadores(List.of(empleador.getId())).size());

    ReporteResena reporte = new ReporteResena();
    reporte.setResena(reciente);
    reporte.setReportante(primeraCandidata);
    reporte.setFecha(LocalDateTime.now());
    repositorio.guardarReporte(reporte);
    sessionFactory.getCurrentSession().flush();
    assertNotNull(reporte.getId());
  }

  private Usuario guardarUsuario(String email, String rol) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword("test");
    usuario.setRol(rol);
    usuario.setNombre("Usuario");
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }

  private Resena guardarResena(
    Usuario empleador,
    Usuario candidato,
    int puntaje,
    LocalDateTime fecha
  ) {
    Resena resena = new Resena();
    resena.setEmpleador(empleador);
    resena.setCandidato(candidato);
    resena.setPuntaje(puntaje);
    resena.setComentario("Comentario de prueba");
    resena.setFecha(fecha);
    sessionFactory.getCurrentSession().persist(resena);
    return resena;
  }
}
