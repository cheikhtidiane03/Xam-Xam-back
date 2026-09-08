package com.xaamxaam.repository;

import com.xaamxaam.domain.conformite.TokenVerificationEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenVerificationEmailRepository extends JpaRepository<TokenVerificationEmail, UUID> {
    Optional<TokenVerificationEmail> findByTokenAndUtiliseFalse(String token);
}
