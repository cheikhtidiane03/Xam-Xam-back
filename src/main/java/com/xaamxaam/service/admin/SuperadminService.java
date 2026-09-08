package com.xaamxaam.service.admin;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.dto.response.VueGlobaleResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EtablissementRepository;
import com.xaamxaam.repository.ExerciceRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logique specifique au role Superadmin : validation manuelle des comptes,
 * moderation, messages globaux, vue d'ensemble du projet (cf. section 12
 * du cahier des charges).
 */
@Service
@RequiredArgsConstructor
public class SuperadminService {

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementRepository etablissementRepository;
    private final ExerciceRepository exerciceRepository;

    public List<Utilisateur> listerComptesEnAttente() {
        return utilisateurRepository.findAll().stream()
                .filter(u -> !u.isActif())
                .toList();
    }

    @Transactional
    public Utilisateur validerCompte(UUID utilisateurId) {
        Utilisateur utilisateur = recupererUtilisateur(utilisateurId);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public Utilisateur suspendreCompte(UUID utilisateurId) {
        Utilisateur utilisateur = recupererUtilisateur(utilisateurId);
        utilisateur.setActif(false);
        return utilisateurRepository.save(utilisateur);
    }

    public VueGlobaleResponse construireVueGlobale() {
        List<Utilisateur> tousLesUtilisateurs = utilisateurRepository.findAll();
        List<Etablissement> etablissements = etablissementRepository.findAll();

        long nombreElevesActifs = tousLesUtilisateurs.stream()
                .filter(u -> u.getRole() == Role.ELEVE && u.isActif())
                .count();

        long etablissementsValides = etablissements.stream()
                .filter(e -> !e.isEnAttenteValidation())
                .count();

        long etablissementsEnAttente = etablissements.stream()
                .filter(Etablissement::isEnAttenteValidation)
                .count();

        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        long exercicesAujourdHui = exerciceRepository.findAll().stream()
                .filter(e -> e.getDateCreation() != null && e.getDateCreation().isAfter(debutJournee))
                .count();

        return new VueGlobaleResponse(
                tousLesUtilisateurs.size(),
                nombreElevesActifs,
                etablissementsValides,
                etablissementsEnAttente,
                exercicesAujourdHui
        );
    }

    private Utilisateur recupererUtilisateur(UUID utilisateurId) {
        return utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
