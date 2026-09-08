package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.Indice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndiceRepository extends JpaRepository<Indice, UUID> {
    List<Indice> findByExerciceIdOrderByNiveauAsc(UUID exerciceId);
    Optional<Indice> findByExerciceIdAndNiveau(UUID exerciceId, int niveau);
}
