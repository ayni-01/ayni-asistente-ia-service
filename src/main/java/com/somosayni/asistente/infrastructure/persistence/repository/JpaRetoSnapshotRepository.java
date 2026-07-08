package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.RetoSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRetoSnapshotRepository extends JpaRepository<RetoSnapshot, String> {
}
