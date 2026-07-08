package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reto")
public class RetoSnapshot {

    @Id
    private String id;

    private String titulo;

    @Column(length = 4000)
    private String descripcion;

    private String categoria;

    @Column(name = "nivel_dificultad")
    private String nivelDificultad;

    private String estado;

    @ElementCollection
    @CollectionTable(name = "reto_requisito", joinColumns = @JoinColumn(name = "reto_id"))
    @Column(name = "descripcion")
    private List<String> requisitos = new ArrayList<>();

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public String getNivelDificultad() { return nivelDificultad; }
    public String getEstado() { return estado; }
    public List<String> getRequisitos() { return requisitos; }
}
