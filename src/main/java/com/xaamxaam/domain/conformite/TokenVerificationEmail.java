package com.xaamxaam.domain.conformite;

import com.xaamxaam.domain.user.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens_verification_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenVerificationEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(nullable = false)
    @Builder.Default
    private boolean utilise = false;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;
}
