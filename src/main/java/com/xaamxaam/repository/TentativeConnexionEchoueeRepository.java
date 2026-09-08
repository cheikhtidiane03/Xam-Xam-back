package com.xaamxaam.repository;

import com.xaamxaam.domain.securite.TentativeConnexionEchouee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TentativeConnexionEchoueeRepository extends JpaRepository<TentativeConnexionEchouee, UUID> {
    long countByEmailAndDateTentativeAfter(String email, LocalDateTime depuis);
    void deleteByDateTentativeBefore(LocalDateTime seuil);
}
