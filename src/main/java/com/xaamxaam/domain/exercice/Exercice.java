package com.xaamxaam.domain.exercice;

import com.xaamxaam.domain.user.Eleve;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enonce;

    /**
     * Si l'exercice a ete saisi par photo, l'URL/reference du fichier
     * source avant passage OCR (traçabilite, pas de re-traitement).
     */
    @Column(name = "source_image_url")
    private String sourceImageUrl;

    @Column(nullable = false)
    private String matiere;

    @Column(nullable = false)
    private String niveau;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutExercice statut = StatutExercice.EN_COURS;

    @OneToMany(mappedBy = "exercice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Tentative> tentatives = new ArrayList<>();

    @OneToMany(mappedBy = "exercice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Indice> indices = new ArrayList<>();

    @OneToOne(mappedBy = "exercice", cascade = CascadeType.ALL, orphanRemoval = true)
    private Reformulation reformulation;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    public enum StatutExercice {
        EN_COURS, REFORMULATION_EN_ATTENTE, TERMINE, ABANDONNE
    }
}
