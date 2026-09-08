package com.xaamxaam.controller;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.exercice.SignalementContournement;
import com.xaamxaam.domain.user.Utilisateur;
import com.xaamxaam.dto.request.EtablissementRequest;
import com.xaamxaam.dto.response.EtablissementResponse;
import com.xaamxaam.dto.response.SignalementContournementResponse;
import com.xaamxaam.dto.response.VueGlobaleResponse;
import com.xaamxaam.mapper.EtablissementMapper;
import com.xaamxaam.service.admin.SuperadminService;
import com.xaamxaam.service.etablissement.EtablissementService;
import com.xaamxaam.service.exercice.SignalementContournementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Module Superadmin, entierement isole sous /api/admin/** et protege par
 * @PreAuthorize au niveau classe (en plus de la regle deja posee dans
 * SecurityConfig) pour reduire au maximum le risque d'oubli sur un endpoint.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPERADMIN')")
@Tag(name = "Superadmin", description = "Validation des inscriptions, gestion des etablissements, moderation, vue globale")
public class SuperadminController {

    private final SuperadminService superadminService;
    private final EtablissementService etablissementService;
    private final EtablissementMapper etablissementMapper;
    private final SignalementContournementService signalementService;

    @GetMapping("/comptes/en-attente")
    @Operation(summary = "Lister les comptes en attente de validation")
    public ResponseEntity<List<Utilisateur>> comptesEnAttente() {
        return ResponseEntity.ok(superadminService.listerComptesEnAttente());
    }

    @PostMapping("/comptes/{utilisateurId}/valider")
    @Operation(summary = "Valider un compte")
    public ResponseEntity<Void> validerCompte(@PathVariable UUID utilisateurId) {
        superadminService.validerCompte(utilisateurId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comptes/{utilisateurId}/suspendre")
    @Operation(summary = "Suspendre un compte", description = "Moderation en cas d'usage abusif ou de contournement du systeme.")
    public ResponseEntity<Void> suspendreCompte(@PathVariable UUID utilisateurId) {
        superadminService.suspendreCompte(utilisateurId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/etablissements")
    @Operation(summary = "Ajouter un etablissement")
    public ResponseEntity<EtablissementResponse> creerEtablissement(@Valid @RequestBody EtablissementRequest request) {
        Etablissement etablissement = etablissementService.creer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(etablissementMapper.toResponse(etablissement));
    }

    @GetMapping("/etablissements")
    @Operation(summary = "Lister tous les etablissements")
    public ResponseEntity<List<EtablissementResponse>> listerEtablissements() {
        List<EtablissementResponse> reponse = etablissementService.listerTous().stream()
                .map(etablissementMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reponse);
    }

    @GetMapping("/etablissements/en-attente")
    @Operation(summary = "Lister les etablissements en attente de validation")
    public ResponseEntity<List<EtablissementResponse>> etablissementsEnAttente() {
        List<EtablissementResponse> reponse = etablissementService.listerEnAttenteValidation().stream()
                .map(etablissementMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/etablissements/{etablissementId}/valider")
    @Operation(summary = "Valider un etablissement et activer sa licence")
    public ResponseEntity<EtablissementResponse> validerEtablissement(@PathVariable UUID etablissementId) {
        return ResponseEntity.ok(etablissementMapper.toResponse(etablissementService.validerEtablissement(etablissementId)));
    }

    @DeleteMapping("/etablissements/{etablissementId}")
    @Operation(summary = "Retirer un etablissement")
    public ResponseEntity<Void> retirerEtablissement(@PathVariable UUID etablissementId) {
        etablissementService.retirerEtablissement(etablissementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vue-globale")
    @Operation(summary = "Vue globale du projet",
            description = "Nombre d'inscrits, taux d'usage, etablissements en attente de validation.")
    public ResponseEntity<VueGlobaleResponse> vueGlobale() {
        return ResponseEntity.ok(superadminService.construireVueGlobale());
    }

    @GetMapping("/signalements-contournement")
    @Operation(summary = "Lister les signalements de contournement non traites",
            description = "Genere automatiquement par ContournementDetectionService (copie d'enonce, reponse anormalement longue, rapidite suspecte). "
                    + "Ne bloque jamais l'eleve : sert uniquement de file de revue.")
    public ResponseEntity<List<SignalementContournementResponse>> signalementsNonTraites() {
        List<SignalementContournementResponse> reponse = signalementService.listerNonTraites().stream()
                .map(this::versReponse)
                .toList();
        return ResponseEntity.ok(reponse);
    }

    @PostMapping("/signalements-contournement/{signalementId}/traiter")
    @Operation(summary = "Marquer un signalement comme traite")
    public ResponseEntity<Void> traiterSignalement(@PathVariable UUID signalementId) {
        signalementService.marquerTraite(signalementId);
        return ResponseEntity.noContent().build();
    }

    private SignalementContournementResponse versReponse(SignalementContournement s) {
        return new SignalementContournementResponse(
                s.getId(),
                s.getEleve().getId(),
                s.getEleve().getPrenom() + " " + s.getEleve().getNom(),
                s.getExercice() != null ? s.getExercice().getId() : null,
                s.getType().name(),
                s.getDetails(),
                s.isTraite(),
                s.getDateCreation()
        );
    }

    // NOTE : endpoint "messages globaux" (diffusion d'annonces a tous les
    // utilisateurs) a ajouter selon le canal retenu (email, notification
    // in-app, etc.) - pas encore precise dans le cahier des charges.
}

