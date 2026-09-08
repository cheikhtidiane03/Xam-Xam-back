package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.Reformulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReformulationRepository extends JpaRepository<Reformulation, UUID> {
    Optional<Reformulation> findByExerciceId(UUID exerciceId);
}
