package com.xaamxaam.domain.exercice;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "indices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Indice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false)
    private Exercice exercice;

    /** Niveau de l'indice : 1, 2 ou 3 (cf. section 4.1 du cahier des charges). */
    @Column(nullable = false)
    private int niveau;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    @Builder.Default
    private boolean debloque = false;

    @Column(name = "date_deblocage")
    private LocalDateTime dateDeblocage;
}
