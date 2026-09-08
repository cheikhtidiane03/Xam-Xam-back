package com.xaamxaam.repository;

import com.xaamxaam.domain.abonnement.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbonnementRepository extends JpaRepository<Abonnement, UUID> {
    List<Abonnement> findByParentId(UUID parentId);
    List<Abonnement> findByEtablissementId(UUID etablissementId);
}
