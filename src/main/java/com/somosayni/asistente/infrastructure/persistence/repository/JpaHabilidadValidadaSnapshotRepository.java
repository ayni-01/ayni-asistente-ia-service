package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.HabilidadValidadaSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaHabilidadValidadaSnapshotRepository extends JpaRepository<HabilidadValidadaSnapshot, String> {
    List<HabilidadValidadaSnapshot> findByTalentoId(String talentoId);
}
