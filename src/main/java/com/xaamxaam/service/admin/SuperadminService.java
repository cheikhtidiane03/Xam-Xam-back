package com.xaamxaam.service.admin;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.dto.response.ExerciceListeResponse;
import com.xaamxaam.dto.response.UtilisateurListeResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperadminService {

    /** Un eleve est "actif" s'il a lance au moins un exercice sur cette periode. */
    private static final int JOURS_ACTIVITE = 30;

    private final UtilisateurRepository utilisateurRepository;
    private final EtablissementRepository etablissementRepository;
    private final ExerciceRepository exerciceRepository;

    public List<Utilisateur> listerComptesEnAttente() {
        return utilisateurRepository.findAll().stream()
                .filter(u -> !u.isActif())
                .toList();
    }

    /** Liste TOUS les utilisateurs, quel que soit leur statut - vue de gestion complete pour le superadmin. */
    public List<Utilisateur> listerTousLesComptes() {
        return utilisateurRepository.findAll();
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
        List<Exercice> exercices = exerciceRepository.findAll();

        LocalDateTime seuilActivite = LocalDateTime.now().minusDays(JOURS_ACTIVITE);
        Map<UUID, LocalDateTime> derniereActiviteParEleve = derniereActiviteParEleve(exercices);

        long nombreElevesActifs = tousLesUtilisateurs.stream()
                .filter(u -> u.getRole() == Role.ELEVE && u.isActif())
                .filter(u -> aEteActif(derniereActiviteParEleve.get(u.getId()), seuilActivite))
                .count();

        long etablissementsValides = etablissements.stream()
                .filter(e -> !e.isEnAttenteValidation())
                .count();

        long etablissementsEnAttente = etablissements.stream()
                .filter(Etablissement::isEnAttenteValidation)
                .count();

        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        long exercicesAujourdHui = exercices.stream()
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

    /**
     * Liste des utilisateurs, avec filtres optionnels.
     *
     * @param role        si non nul, ne garde que ce role
     * @param actifRecent si vrai, ne garde que les comptes actifs ayant lance
     *                    au moins un exercice sur les 30 derniers jours
     */
    public List<UtilisateurListeResponse> listerUtilisateurs(Role role, boolean actifRecent) {
        List<Exercice> exercices = exerciceRepository.findAll();
        Map<UUID, LocalDateTime> derniereActivite = derniereActiviteParEleve(exercices);
        Map<UUID, Long> nombreParEleve = exercices.stream()
                .filter(e -> e.getEleve() != null)
                .collect(Collectors.groupingBy(e -> e.getEleve().getId(), Collectors.counting()));
        LocalDateTime seuilActivite = LocalDateTime.now().minusDays(JOURS_ACTIVITE);

        return utilisateurRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> !actifRecent
                        || (u.isActif() && aEteActif(derniereActivite.get(u.getId()), seuilActivite)))
                .map(u -> new UtilisateurListeResponse(
                        u.getId(),
                        u.getPrenom(),
                        u.getNom(),
                        u.getEmail(),
                        u.getRole().name(),
                        u.isActif(),
                        u.getDateCreation(),
                        derniereActivite.get(u.getId()),
                        nombreParEleve.getOrDefault(u.getId(), 0L)
                ))
                .sorted(Comparator.comparing(UtilisateurListeResponse::derniereActivite,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Liste des exercices, du plus recent au plus ancien. */
    public List<ExerciceListeResponse> listerExercices(boolean aujourdhuiSeulement) {
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        return exerciceRepository.findAll().stream()
                .filter(e -> e.getDateCreation() != null)
                .filter(e -> !aujourdhuiSeulement || e.getDateCreation().isAfter(debutJournee))
                .sorted(Comparator.comparing(Exercice::getDateCreation).reversed())
                .map(e -> new ExerciceListeResponse(
                        e.getId(),
                        e.getEleve() != null ? e.getEleve().getId() : null,
                        e.getEleve() != null ? e.getEleve().getPrenom() + " " + e.getEleve().getNom() : "—",
                        e.getDateCreation()
                ))
                .toList();
    }

    private Map<UUID, LocalDateTime> derniereActiviteParEleve(List<Exercice> exercices) {
        return exercices.stream()
                .filter(e -> e.getEleve() != null && e.getDateCreation() != null)
                .collect(Collectors.toMap(
                        e -> e.getEleve().getId(),
                        Exercice::getDateCreation,
                        (a, b) -> a.isAfter(b) ? a : b
                ));
    }

    private boolean aEteActif(LocalDateTime derniereActivite, LocalDateTime seuil) {
        return derniereActivite != null && derniereActivite.isAfter(seuil);
    }

    private Utilisateur recupererUtilisateur(UUID utilisateurId) {
        return utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}