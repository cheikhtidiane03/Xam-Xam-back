package com.xaamxaam.controller;

import com.xaamxaam.domain.user.AdminEtablissement;
import com.xaamxaam.dto.request.CreerEleveParAdminRequest;
import com.xaamxaam.dto.request.CreerEnseignantParAdminRequest;
import com.xaamxaam.dto.response.CompteEleveResponse;
import com.xaamxaam.dto.response.CompteEnseignantResponse;
import com.xaamxaam.dto.response.EleveStatNominativeResponse;
import com.xaamxaam.dto.response.StatistiquesClasseResponse;
import com.xaamxaam.service.etablissement.GestionComptesEtablissementService;
import com.xaamxaam.service.suivi.StatistiquesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Toutes les operations de ce controleur sont automatiquement scopees a
 * l'etablissement du compte AdminEtablissement authentifie (voir
 * GestionComptesEtablissementService) : un admin ne peut jamais agir sur un
 * autre etablissement, meme en falsifiant un ID dans la requete.
 */
@RestController
@RequestMapping("/api/admin-etablissement")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN_ETABLISSEMENT')")
@Tag(name = "Administrateur etablissement", description = "Gestion des comptes eleves/enseignants et suivi de la licence")
public class AdminEtablissementController {

    private final StatistiquesService statistiquesService;
    private final GestionComptesEtablissementService gestionComptesService;

    @GetMapping("/statistiques")
    @Operation(summary = "Consulter les statistiques de mon etablissement")
    public ResponseEntity<StatistiquesClasseResponse> statistiques(@AuthenticationPrincipal AdminEtablissement admin) {
        return ResponseEntity.ok(statistiquesService.construireStatistiquesEtablissement(
                admin.getEtablissement().getId()
        ));
    }

    @GetMapping("/statistiques/nominatives")
    @Operation(summary = "Consulter les statistiques nominatives des eleves ayant consenti",
            description = "Ne liste QUE les eleves dont le parent a explicitement autorise les statistiques nominatives.")
    public ResponseEntity<List<EleveStatNominativeResponse>> statistiquesNominatives(@AuthenticationPrincipal AdminEtablissement admin) {
        return ResponseEntity.ok(statistiquesService.construireStatistiquesNominatives(
                admin.getEtablissement().getId()
        ));
    }

    // ===== Gestion des comptes eleves =====

    @PostMapping("/eleves")
    @Operation(summary = "Creer un compte eleve",
            description = "L'eleve est automatiquement rattache a l'etablissement de l'administrateur connecte et active immediatement.")
    public ResponseEntity<CompteEleveResponse> creerEleve(
            @AuthenticationPrincipal AdminEtablissement admin,
            @Valid @RequestBody CreerEleveParAdminRequest request
    ) {
        CompteEleveResponse reponse = gestionComptesService.creerEleve(admin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping("/eleves")
    @Operation(summary = "Lister les eleves de mon etablissement")
    public ResponseEntity<List<CompteEleveResponse>> listerEleves(@AuthenticationPrincipal AdminEtablissement admin) {
        return ResponseEntity.ok(gestionComptesService.listerEleves(admin));
    }

    @PostMapping("/eleves/{eleveId}/desactiver")
    @Operation(summary = "Desactiver un compte eleve de mon etablissement")
    public ResponseEntity<Void> desactiverEleve(
            @AuthenticationPrincipal AdminEtablissement admin,
            @PathVariable UUID eleveId
    ) {
        gestionComptesService.basculerActivationEleve(admin, eleveId, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/eleves/{eleveId}/reactiver")
    @Operation(summary = "Reactiver un compte eleve de mon etablissement")
    public ResponseEntity<Void> reactiverEleve(
            @AuthenticationPrincipal AdminEtablissement admin,
            @PathVariable UUID eleveId
    ) {
        gestionComptesService.basculerActivationEleve(admin, eleveId, true);
        return ResponseEntity.noContent().build();
    }

    // ===== Gestion des comptes enseignants =====

    @PostMapping("/enseignants")
    @Operation(summary = "Creer un compte enseignant",
            description = "L'enseignant est automatiquement rattache a l'etablissement de l'administrateur connecte et active immediatement.")
    public ResponseEntity<CompteEnseignantResponse> creerEnseignant(
            @AuthenticationPrincipal AdminEtablissement admin,
            @Valid @RequestBody CreerEnseignantParAdminRequest request
    ) {
        CompteEnseignantResponse reponse = gestionComptesService.creerEnseignant(admin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping("/enseignants")
    @Operation(summary = "Lister les enseignants de mon etablissement")
    public ResponseEntity<List<CompteEnseignantResponse>> listerEnseignants(@AuthenticationPrincipal AdminEtablissement admin) {
        return ResponseEntity.ok(gestionComptesService.listerEnseignants(admin));
    }

    @PostMapping("/enseignants/{enseignantId}/desactiver")
    @Operation(summary = "Desactiver un compte enseignant de mon etablissement")
    public ResponseEntity<Void> desactiverEnseignant(
            @AuthenticationPrincipal AdminEtablissement admin,
            @PathVariable UUID enseignantId
    ) {
        gestionComptesService.basculerActivationEnseignant(admin, enseignantId, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/enseignants/{enseignantId}/reactiver")
    @Operation(summary = "Reactiver un compte enseignant de mon etablissement")
    public ResponseEntity<Void> reactiverEnseignant(
            @AuthenticationPrincipal AdminEtablissement admin,
            @PathVariable UUID enseignantId
    ) {
        gestionComptesService.basculerActivationEnseignant(admin, enseignantId, true);
        return ResponseEntity.noContent().build();
    }
}
