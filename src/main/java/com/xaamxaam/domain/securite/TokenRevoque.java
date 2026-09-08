package com.xaamxaam.domain.securite;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Liste noire des JWT revoques suite a un logout explicite. Le champ jti
 * (JWT ID) identifie de facon unique le token concerne (voir JwtService).
 */
@Entity
@Table(name = "tokens_revoques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRevoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @CreationTimestamp
    @Column(name = "date_revocation", updatable = false)
    private LocalDateTime dateRevocation;
}
