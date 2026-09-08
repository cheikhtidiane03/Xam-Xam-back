package com.xaamxaam.repository;

import com.xaamxaam.domain.user.AdminEtablissement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminEtablissementRepository extends JpaRepository<AdminEtablissement, UUID> {
}
