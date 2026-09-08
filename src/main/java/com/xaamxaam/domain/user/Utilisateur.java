package com.xaamxaam.domain.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Classe mere de la hierarchie des utilisateurs (Eleve, Parent, Enseignant,
 * AdminEtablissement, Superadmin). Implemente UserDetails pour s'integrer
 * directement a Spring Security.
 *
 * Strategie d'heritage JOINED : une table "utilisateurs" avec les champs
 * communs + une table par sous-type pour ses champs specifiques. Plus
 * normalise que SINGLE_TABLE, au prix d'une jointure par requete.
 */
@Entity
@Table(name = "utilisateurs")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@JsonIgnoreProperties({"authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "username"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Utilisateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, insertable = false, updatable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Compte actif ou non. Utilise pour :
     * - la validation manuelle des inscriptions par le superadmin (phase de lancement)
     * - la moderation (suspension d'un compte en cas d'abus)
     */
    @Column(nullable = false)
    @lombok.Builder.Default
    private boolean actif = false;

    /** Verification d'email a l'inscription (cf. TokenVerificationEmail / EmailVerificationService). */
    @Column(name = "email_verifie", nullable = false)
    @lombok.Builder.Default
    private boolean emailVerifie = false;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    // ===== Implementation UserDetails =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
