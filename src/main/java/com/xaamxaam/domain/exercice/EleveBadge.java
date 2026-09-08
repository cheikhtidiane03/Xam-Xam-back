package com.xaamxaam.domain.exercice;

import com.xaamxaam.domain.user.Eleve;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "eleve_badges", uniqueConstraints = @UniqueConstraint(columnNames = {"eleve_id", "badge_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EleveBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @CreationTimestamp
    @Column(name = "date_obtention", updatable = false)
    private LocalDateTime dateObtention;
}
