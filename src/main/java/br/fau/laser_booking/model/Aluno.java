package br.fau.laser_booking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String senhaHash;

    // semestre do aluno (3,4,5,6,7,8,...)
    private Integer semestre;

    // tipo do aluno para as regras de prioridade: TFG ou REGULAR
    @Enumerated(EnumType.STRING)
    private TipoTrabalho tipoTrabalho;

    public enum TipoTrabalho {
        TFG,
        REGULAR
    }

    // =========================
    // Getters e Setters
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Integer getSemestre() {
        return semestre;
    }

    public void setSemestre(Integer semestre) {
        this.semestre = semestre;
    }

    public TipoTrabalho getTipoTrabalho() {
        return tipoTrabalho;
    }

    public void setTipoTrabalho(TipoTrabalho tipoTrabalho) {
        this.tipoTrabalho = tipoTrabalho;
    }
}
