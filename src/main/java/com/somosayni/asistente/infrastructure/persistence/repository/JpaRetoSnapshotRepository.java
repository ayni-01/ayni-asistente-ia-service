package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRetoSnapshotRepository extends JpaRepository<RetoSnapshot, String> {
    List<RetoSnapshot> findByEstado(String estado);
}
