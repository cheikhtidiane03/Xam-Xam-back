package com.xaamxaam.domain.etablissement;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "etablissements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEtablissement type;

    private String adresse;

    @Column(name = "licence_active", nullable = false)
    @Builder.Default
    private boolean licenceActive = false;

    @Column(name = "date_expiration_licence")
    private LocalDate dateExpirationLicence;

    /**
     * En attente de validation manuelle par le superadmin (cf. section 12
     * du cahier des charges : controle manuel pendant la phase de lancement).
     */
    @Column(name = "en_attente_validation", nullable = false)
    @Builder.Default
    private boolean enAttenteValidation = true;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    public enum TypeEtablissement {
        COLLEGE, LYCEE, COLLEGE_LYCEE
    }
}
