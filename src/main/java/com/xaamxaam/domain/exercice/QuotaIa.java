package com.xaamxaam.domain.exercice;

import com.xaamxaam.domain.user.Eleve;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Compteur de requetes IA par eleve et par jour, utilise pour maitriser le
 * cout de l'API Claude (cf. section 7 du cahier des charges : "fixer une
 * limite de requetes gratuites par eleve"). Une ligne par (eleve, date).
 */
@Entity
@Table(name = "quotas_ia", uniqueConstraints = @UniqueConstraint(columnNames = {"eleve_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaIa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "nombre_requetes", nullable = false)
    @Builder.Default
    private int nombreRequetes = 0;
}
