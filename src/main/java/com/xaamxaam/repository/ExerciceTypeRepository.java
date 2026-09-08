package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.ExerciceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciceTypeRepository extends JpaRepository<ExerciceType, UUID> {
    List<ExerciceType> findByMatiereIgnoreCaseAndNiveauIgnoreCase(String matiere, String niveau);
    List<ExerciceType> findByNiveauIgnoreCase(String niveau);
}
