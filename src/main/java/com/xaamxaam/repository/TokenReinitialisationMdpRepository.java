package com.xaamxaam.repository;

import com.xaamxaam.domain.conformite.TokenReinitialisationMdp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenReinitialisationMdpRepository extends JpaRepository<TokenReinitialisationMdp, UUID> {
    Optional<TokenReinitialisationMdp> findByTokenAndUtiliseFalse(String token);
}
