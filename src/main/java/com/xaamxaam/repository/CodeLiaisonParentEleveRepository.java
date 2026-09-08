package com.xaamxaam.repository;

import com.xaamxaam.domain.user.CodeLiaisonParentEleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CodeLiaisonParentEleveRepository extends JpaRepository<CodeLiaisonParentEleve, UUID> {
    Optional<CodeLiaisonParentEleve> findByCodeAndUtiliseFalse(String code);
    void deleteByDateExpirationBefore(LocalDateTime seuil);
}
