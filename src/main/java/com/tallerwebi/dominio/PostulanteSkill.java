package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "Postulante_skill",
  uniqueConstraints = @UniqueConstraint(columnNames = { "postulante_id", "skill_id" })
)
public class PostulanteSkill {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "postulante_id", nullable = false)
  private PerfilPostulante postulante;

  @ManyToOne
  @JoinColumn(name = "skill_id", nullable = false)
  private Skill skill;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public PerfilPostulante getPostulante() {
    return postulante;
  }

  public void setPostulante(PerfilPostulante postulante) {
    this.postulante = postulante;
  }

  public Skill getSkill() {
    return skill;
  }

  public void setSkill(Skill skill) {
    this.skill = skill;
  }
}
