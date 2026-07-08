package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "habilidad_validada")
public class HabilidadValidadaSnapshot {

    @Id
    private String id;

    @Column(name = "talento_id")
    private String talentoId;

    private String nombre;

    private String nivel;

    private int porcentaje;

    public String getId() { return id; }
    public String getTalentoId() { return talentoId; }
    public String getNombre() { return nombre; }
    public String getNivel() { return nivel; }
    public int getPorcentaje() { return porcentaje; }
}
