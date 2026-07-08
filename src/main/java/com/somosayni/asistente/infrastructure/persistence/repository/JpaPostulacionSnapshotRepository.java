package com.somosayni.asistente.infrastructure.persistence.repository;

import com.somosayni.asistente.infrastructure.persistence.entity.PostulacionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPostulacionSnapshotRepository extends JpaRepository<PostulacionSnapshot, String> {
}
