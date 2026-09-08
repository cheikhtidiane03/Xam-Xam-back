package com.xaamxaam.repository;

import com.xaamxaam.domain.user.Superadmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SuperadminRepository extends JpaRepository<Superadmin, UUID> {
}
