package com.xaamxaam.repository;

import com.xaamxaam.domain.securite.TokenRevoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TokenRevoqueRepository extends JpaRepository<TokenRevoque, UUID> {
    boolean existsByJti(String jti);
    void deleteByDateExpirationBefore(LocalDateTime seuil);
}
