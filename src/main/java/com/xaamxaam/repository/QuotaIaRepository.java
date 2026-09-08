package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.QuotaIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface QuotaIaRepository extends JpaRepository<QuotaIa, UUID> {

    /**
     * Verrou pessimiste pour eviter une condition de course si l'eleve
     * envoie plusieurs requetes IA en parallele (ex : plusieurs onglets).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QuotaIa q WHERE q.eleve.id = :eleveId AND q.date = :date")
    Optional<QuotaIa> findForUpdateByEleveIdAndDate(UUID eleveId, LocalDate date);

    Optional<QuotaIa> findByEleveIdAndDate(UUID eleveId, LocalDate date);
}
