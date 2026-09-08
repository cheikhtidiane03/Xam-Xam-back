package com.xaamxaam.controller;

import com.xaamxaam.domain.abonnement.Abonnement;
import com.xaamxaam.domain.user.Parent;
import com.xaamxaam.dto.request.ConsentementRequest;
import com.xaamxaam.dto.request.RelierEnfantRequest;
import com.xaamxaam.dto.response.CompteEleveResponse;
import com.xaamxaam.dto.response.ConsentementResponse;
import com.xaamxaam.dto.response.DonneesEleveExportResponse;
import com.xaamxaam.dto.response.EleveSuiviResponse;
import com.xaamxaam.dto.response.ProgressionMatiereResponse;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.service.abonnement.AbonnementService;
import com.xaamxaam.service.conformite.ConsentementService;
import com.xaamxaam.service.conformite.GdprService;
import com.xaamxaam.service.parent.LiaisonParentEleveService;
import com.xaamxaam.service.suivi.TableauDeBordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Parent", description = "Rattachement des enfants, suivi de l'effort de reflexion, consentement et abonnement")
public class ParentController {

    private final TableauDeBordService tableauDeBordService;
    private final AbonnementService abonnementService;
    private final ConsentementService consentementService;
    private final GdprService gdprService;
    private final LiaisonParentEleveService liaisonParentEleveService;

    @PostMapping("/enfants")
    @Operation(summary = "Rattacher un enfant via son code de liaison",
            description = "Le code est genere par l'eleve depuis son propre compte "
                    + "(GET /api/eleve/code-liaison-parent) puis partage avec son parent. "
                    + "Ce sens unique (eleve -> parent) evite qu'un inconnu se rattache a un compte de mineur "
                    + "juste en connaissant son adresse email.")
    public ResponseEntity<CompteEleveResponse> relierEnfant(
            @AuthenticationPrincipal Parent parent,
            @Valid @RequestBody RelierEnfantRequest request
    ) {
        CompteEleveResponse reponse = liaisonParentEleveService.relierAvecCode(parent, request.code());
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping("/enfants")
    @Operation(summary = "Lister mes enfants rattaches")
    public ResponseEntity<List<CompteEleveResponse>> mesEnfants(@AuthenticationPrincipal Parent parent) {
        return ResponseEntity.ok(liaisonParentEleveService.listerEnfants(parent.getId()));
    }

    @DeleteMapping("/enfants/{eleveId}")
    @Operation(summary = "Detacher un enfant de mon compte")
    public ResponseEntity<Void> delierEnfant(
            @AuthenticationPrincipal Parent parent,
            @PathVariable UUID eleveId
    ) {
        liaisonParentEleveService.delierEnfant(parent, eleveId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/enfants/{eleveId}/suivi")
    @Operation(summary = "Consulter le tableau de suivi d'un enfant",
            description = "Verifie que l'eleve consulte fait bien partie des enfants rattaches a ce parent.")
    public ResponseEntity<EleveSuiviResponse> suiviEnfant(
            @AuthenticationPrincipal Parent parent,
            @PathVariable UUID eleveId
    ) {
        verifierEnfantDuParent(parent, eleveId);
        return ResponseEntity.ok(tableauDeBordService.construireSuiviEleve(eleveId));
    }

    @GetMapping("/enfants/{eleveId}/progression")
    @Operation(summary = "Consulter la progression par matiere d'un enfant")
    public ResponseEntity<List<ProgressionMatiereResponse>> progressionEnfant(
            @AuthenticationPrincipal Parent parent,
            @PathVariable UUID eleveId
    ) {
        verifierEnfantDuParent(parent, eleveId);
        return ResponseEntity.ok(tableauDeBordService.construireProgressionParMatiere(eleveId));
    }

    @GetMapping("/enfants/{eleveId}/consentement")
    @Operation(summary = "Consulter l'etat du consentement pour un enfant")
    public ResponseEntity<ConsentementResponse> consulterConsentement(
            @AuthenticationPrincipal Parent parent,
            @PathVariable UUID eleveId
    ) {
        verifierEnfantDuParent(parent, eleveId);
        return ResponseEntity.ok(consentementService.consulterConsentement(eleveId));
    }

    @PutMapping("/enfants/{eleveId}/consentement")
    @Operation(summary = "Mettre a jour le consentement parental pour un enfant",
            description = "Deux consentements distincts : traitement general des donnees, et autorisation "
                    + "pour que l'etablissement voie des statistiques nominatives (nom + score) plutot qu'agregees.")
    public ResponseEntity<ConsentementResponse> mettreAJourConsentement(
            @AuthenticationPrincipal Parent parent,
            @PathVariable UUID eleveId,
            @Valid @RequestBody ConsentementRequest request
    ) {
        verifierEnfantDuParent(parent, eleveId);
        return ResponseEntity.ok(consentementService.mettreAJourConsentement(parent, eleveId, request));
    }

    @GetMapping("/enfants/{eleveId}/export-donnees")
    @Operation(summary = "Exporter les donnees personnelles d'un enfant",
            description = "Droit d'acces / portabilite exerce au nom de l'enfant mineur.")
    public ResponseEntity<DonneesEleveExportResponse> exporterDonneesEnfant(
            @AuthenticationPrincipal Parent parent,
            @PathVariable UUID eleveId
    ) {
        verifierEnfantDuParent(parent, eleveId);
        return ResponseEntity.ok(gdprService.exporterDonnees(eleveId));
    }

    @GetMapping("/abonnement")
    @Operation(summary = "Consulter mon abonnement")
    public ResponseEntity<List<Abonnement>> monAbonnement(@AuthenticationPrincipal Parent parent) {
        return ResponseEntity.ok(abonnementService.listerParParent(parent.getId()));
    }

    private void verifierEnfantDuParent(Parent parent, UUID eleveId) {
        boolean estEnfantDuParent = parent.getEleves().stream()
                .anyMatch(e -> e.getId().equals(eleveId));
        if (!estEnfantDuParent) {
            throw new AccesRefuseException("Cet eleve n'est pas rattache a ce compte parent.");
        }
    }
}
