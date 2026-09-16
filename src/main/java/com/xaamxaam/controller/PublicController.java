package com.xaamxaam.controller;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.dto.response.EtablissementPublicResponse;
import com.xaamxaam.service.etablissement.EtablissementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints accessibles SANS authentification, utilises par exemple par le
 * formulaire d'inscription (qui doit pouvoir proposer une liste
 * d'etablissements avant meme que l'utilisateur ait un compte).
 * Ne jamais y exposer de donnees sensibles (adresse, statut de licence,
 * emails...) - uniquement ce qui est strictement necessaire au public.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints accessibles sans authentification")
public class PublicController {

    private final EtablissementService etablissementService;

    @GetMapping("/etablissements")
    @Operation(summary = "Lister les etablissements valides",
            description = "Utilise par le formulaire d'inscription pour proposer un etablissement aux comptes Enseignant/Admin etablissement. "
                    + "Ne retourne que les etablissements valides par le superadmin (jamais ceux en attente).")
    public ResponseEntity<List<EtablissementPublicResponse>> listerEtablissements() {
        List<EtablissementPublicResponse> reponse = etablissementService.listerPublics().stream()
                .map(this::versReponsePublique)
                .toList();
        return ResponseEntity.ok(reponse);
    }

    private EtablissementPublicResponse versReponsePublique(Etablissement etablissement) {
        return new EtablissementPublicResponse(etablissement.getId(), etablissement.getNom(), etablissement.getType());
    }
}