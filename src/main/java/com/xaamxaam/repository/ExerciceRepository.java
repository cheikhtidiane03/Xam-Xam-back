package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.user.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciceRepository extends JpaRepository<Exercice, UUID> {
    List<Exercice> findByEleveOrderByDateCreationDesc(Eleve eleve);
    List<Exercice> findByEleveIdOrderByDateCreationDesc(UUID eleveId);
}
