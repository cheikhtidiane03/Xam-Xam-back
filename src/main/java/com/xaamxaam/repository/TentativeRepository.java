package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.Tentative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TentativeRepository extends JpaRepository<Tentative, UUID> {
    List<Tentative> findByExerciceIdOrderByDateHeureAsc(UUID exerciceId);
    long countByExerciceId(UUID exerciceId);
}
