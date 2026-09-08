package com.xaamxaam.repository;

import com.xaamxaam.domain.user.Parent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParentRepository extends JpaRepository<Parent, UUID> {
}
