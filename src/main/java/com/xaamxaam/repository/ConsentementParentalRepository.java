package com.xaamxaam.repository;

import com.xaamxaam.domain.conformite.ConsentementParental;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsentementParentalRepository extends JpaRepository<ConsentementParental, UUID> {
    Optional<ConsentementParental> findByEleveId(UUID eleveId);
}
