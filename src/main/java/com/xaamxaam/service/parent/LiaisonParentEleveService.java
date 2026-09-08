package com.xaamxaam.service.parent;

import com.xaamxaam.domain.user.CodeLiaisonParentEleve;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Parent;
import com.xaamxaam.dto.response.CodeLiaisonResponse;
import com.xaamxaam.dto.response.CompteEleveResponse;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.exception.TokenInvalideException;
import com.xaamxaam.repository.CodeLiaisonParentEleveRepository;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Rattachement d'un Parent a un Eleve. Le flux est TOUJOURS initie par
 * l'eleve (qui genere un code et le partage avec son parent), jamais par
 * le parent via l'email de l'eleve - voir le commentaire de conception
 * dans CodeLiaisonParentEleve pour la justification (protection des
 * donnees des mineurs : empecher qu'un inconnu se rattache a un compte
 * eleve juste en connaissant son adresse email).
 */
@Service
@RequiredArgsConstructor
public class LiaisonParentEleveService {

    private final CodeLiaisonParentEleveRepository codeRepository;
    private final EleveRepository eleveRepository;
    private final ParentRepository parentRepository;

    private static final long EXPIRATION_MINUTES = 30;
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // sans 0/O/1/I/L, ambigus a recopier
    private static final int LONGUEUR_CODE = 8;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public CodeLiaisonResponse genererCode(UUID eleveId) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));

        LocalDateTime expiration = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        CodeLiaisonParentEleve code = CodeLiaisonParentEleve.builder()
                .eleve(eleve)
                .code(genererCodeAleatoire())
                .dateExpiration(expiration)
                .build();

        codeRepository.save(code);
        return new CodeLiaisonResponse(code.getCode(), expiration);
    }

    @Transactional
    public CompteEleveResponse relierAvecCode(Parent parentAuthentifie, String codeSaisi) {
        CodeLiaisonParentEleve code = codeRepository.findByCodeAndUtiliseFalse(codeSaisi.trim().toUpperCase())
                .orElseThrow(() -> new TokenInvalideException("Code de liaison invalide ou deja utilise."));

        if (code.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new TokenInvalideException("Ce code a expire. Demande a l'eleve d'en generer un nouveau.");
        }

        // Le Parent issu du contexte de securite peut etre detache du
        // Persistence Context courant : on le recharge pour pouvoir
        // modifier sa collection eleves de facon geree par Hibernate.
        Parent parent = parentRepository.findById(parentAuthentifie.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent introuvable"));

        Eleve eleve = code.getEleve();

        boolean dejaRattache = parent.getEleves().stream().anyMatch(e -> e.getId().equals(eleve.getId()));
        if (dejaRattache) {
            throw new IllegalArgumentException("Cet eleve est deja rattache a ce compte parent.");
        }

        parent.getEleves().add(eleve);
        parentRepository.save(parent);

        code.setUtilise(true);
        codeRepository.save(code);

        return versReponse(eleve);
    }

    public List<CompteEleveResponse> listerEnfants(UUID parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent introuvable"));

        return parent.getEleves().stream().map(this::versReponse).toList();
    }

    @Transactional
    public void delierEnfant(Parent parentAuthentifie, UUID eleveId) {
        Parent parent = parentRepository.findById(parentAuthentifie.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent introuvable"));

        boolean supprime = parent.getEleves().removeIf(e -> e.getId().equals(eleveId));
        if (!supprime) {
            throw new AccesRefuseException("Cet eleve n'est pas rattache a ce compte parent.");
        }
        parentRepository.save(parent);
    }

    /** Purge quotidienne des codes expires pour ne pas faire grossir la table indefiniment. */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgerCodesExpires() {
        codeRepository.deleteByDateExpirationBefore(LocalDateTime.now());
    }

    private String genererCodeAleatoire() {
        StringBuilder sb = new StringBuilder(LONGUEUR_CODE);
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private CompteEleveResponse versReponse(Eleve eleve) {
        return new CompteEleveResponse(
                eleve.getId(), eleve.getNom(), eleve.getPrenom(), eleve.getEmail(),
                eleve.getNiveauScolaire(), eleve.isActif()
        );
    }
}
