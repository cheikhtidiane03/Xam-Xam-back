package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.EleveBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EleveBadgeRepository extends JpaRepository<EleveBadge, UUID> {
    List<EleveBadge> findByEleveIdOrderByDateObtentionDesc(UUID eleveId);
    boolean existsByEleveIdAndBadgeCode(UUID eleveId, String badgeCode);
}
