package com.xaamxaam.domain.securite;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** Trace des echecs de connexion, utilisee par LoginAttemptService pour le blocage temporaire. */
@Entity
@Table(name = "tentatives_connexion_echouees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TentativeConnexionEchouee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @CreationTimestamp
    @Column(name = "date_tentative", updatable = false)
    private LocalDateTime dateTentative;
}
