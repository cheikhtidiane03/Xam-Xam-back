package com.xaamxaam.repository;

import com.xaamxaam.domain.user.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EnseignantRepository extends JpaRepository<Enseignant, UUID> {
}
