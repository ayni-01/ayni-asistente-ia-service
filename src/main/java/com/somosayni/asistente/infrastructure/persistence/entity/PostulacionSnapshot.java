package com.somosayni.asistente.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "postulacion")
public class PostulacionSnapshot {

    @Id
    private String id;

    @Column(name = "talento_id")
    private String talentoId;

    @Column(name = "reto_id")
    private String retoId;

    public String getId() { return id; }
    public String getTalentoId() { return talentoId; }
    public String getRetoId() { return retoId; }
}
