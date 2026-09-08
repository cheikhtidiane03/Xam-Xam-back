package com.xaamxaam.controller;

import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.ExerciceType;
import com.xaamxaam.domain.exercice.Indice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.domain.exercice.Tentative;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.dto.request.ExerciceRequest;
import com.xaamxaam.dto.request.ReformulationRequest;
import com.xaamxaam.dto.request.TentativeRequest;
import com.xaamxaam.dto.response.*;
import com.xaamxaam.mapper.ExerciceMapper;
import com.xaamxaam.service.conformite.GdprService;
import com.xaamxaam.service.exercice.*;
import com.xaamxaam.service.parent.LiaisonParentEleveService;
import com.xaamxaam.service.suivi.TableauDeBordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/eleve")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Eleve", description = "Parcours guide : saisie d'exercice, tentatives, indices, reformulation finale")
public class EleveController {

    private final ExerciceService exerciceService;
    private final TableauDeBordService tableauDeBordService;
    private final ExerciceMapper exerciceMapper;
    private final OcrService ocrService;
    private final QuotaIaService quotaIaService;
    private final ExerciceTypeService exerciceTypeService;
    private final BadgeService badgeService;
    private final GdprService gdprService;
    private final LiaisonParentEleveService liaisonParentEleveService;

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extraire le texte d'une photo d'exercice",
            description = "Envoie une photo (JPEG/PNG, 5 Mo max) et recoit le texte extrait par OCR. "
                    + "Ce texte est ensuite a utiliser comme 'enonce' dans POST /api/eleve/exercices.")
    public ResponseEntity<OcrResponse> extraireTexteDepuisPhoto(
            @AuthenticationPrincipal Eleve eleve,
            @RequestParam("image") MultipartFile image
    ) {
        String texte = ocrService.extraireTexte(image);
        return ResponseEntity.ok(new OcrResponse(texte));
    }

    @PostMapping("/exercices")
    @Operation(summary = "Soumettre un nouvel exercice (texte libre ou texte issu d'un OCR)")
    public ResponseEntity<ExerciceResponse> creerExercice(
            @AuthenticationPrincipal Eleve eleve,
            @Valid @RequestBody ExerciceRequest request
    ) {
        Exercice exercice = exerciceService.creerExercice(eleve.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciceMapper.toResponse(exercice));
    }

    @PostMapping("/exercices/depuis-bibliotheque/{exerciceTypeId}")
    @Operation(summary = "Creer un exercice a partir d'un modele de la bibliotheque",
            description = "Importe l'enonce, la matiere et le niveau d'un exercice type pour demarrer rapidement un parcours guide.")
    public ResponseEntity<ExerciceResponse> creerExerciceDepuisModele(
            @AuthenticationPrincipal Eleve eleve,
            @PathVariable UUID exerciceTypeId
    ) {
        ExerciceType modele = exerciceTypeService.recuperer(exerciceTypeId);
        ExerciceRequest request = new ExerciceRequest(modele.getEnonce(), modele.getMatiere(), modele.getNiveau(), null);
        Exercice exercice = exerciceService.creerExercice(eleve.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciceMapper.toResponse(exercice));
    }

    @GetMapping("/exercices")
    @Operation(summary = "Lister mes exercices")
    public ResponseEntity<List<ExerciceResponse>> listerExercices(@AuthenticationPrincipal Eleve eleve) {
        List<ExerciceResponse> reponse = exerciceService.listerExercicesEleve(eleve.getId()).stream()
                .map(exerciceMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reponse);
    }

    @GetMapping("/exercices/{exerciceId}")
    @Operation(summary = "Consulter un exercice")
    public ResponseEntity<ExerciceResponse> recupererExercice(
            @AuthenticationPrincipal Eleve eleve,
            @PathVariable UUID exerciceId
    ) {
        Exercice exercice = exerciceService.recupererExercice(exerciceId, eleve.getId());
        return ResponseEntity.ok(exerciceMapper.toResponse(exercice));
    }

    @PostMapping("/exercices/{exerciceId}/tentatives")
    @Operation(summary = "Soumettre une tentative",
            description = "L'IA repond par une question de relance socratique, jamais par la reponse finale.")
    public ResponseEntity<TentativeResponse> soumettreTentative(
            @AuthenticationPrincipal Eleve eleve,
            @PathVariable UUID exerciceId,
            @Valid @RequestBody TentativeRequest request
    ) {
        Tentative tentative = exerciceService.soumettreTentative(exerciceId, eleve.getId(), request);
        TentativeResponse reponse = new TentativeResponse(
                tentative.getId(), tentative.getRelanceIa(), tentative.getIndiceNiveau(), tentative.getDateHeure()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/exercices/{exerciceId}/indices/suivant")
    @Operation(summary = "Debloquer l'indice suivant",
            description = "Debloque le prochain indice non encore utilise (niveau 1, puis 2, puis 3).")
    public ResponseEntity<IndiceResponse> debloquerIndiceSuivant(
            @AuthenticationPrincipal Eleve eleve,
            @PathVariable UUID exerciceId
    ) {
        Indice indice = exerciceService.debloquerIndiceSuivant(exerciceId, eleve.getId());
        return ResponseEntity.ok(exerciceMapper.toResponse(indice));
    }

    @PostMapping("/exercices/{exerciceId}/reformulation")
    @Operation(summary = "Soumettre la reformulation finale (methode Feynman)",
            description = "Etape obligatoire de cloture de l'exercice ; l'IA evalue la comprehension reelle et retourne un score sur 100. "
                    + "Peut declencher l'obtention de nouveaux badges.")
    public ResponseEntity<ReformulationResponse> soumettreReformulation(
            @AuthenticationPrincipal Eleve eleve,
            @PathVariable UUID exerciceId,
            @Valid @RequestBody ReformulationRequest request
    ) {
        Reformulation reformulation = exerciceService.soumettreReformulation(exerciceId, eleve.getId(), request);
        badgeService.evaluerBadges(eleve.getId());
        return ResponseEntity.ok(exerciceMapper.toResponse(reformulation));
    }

    @GetMapping("/tableau-de-bord")
    @Operation(summary = "Consulter mon tableau de bord",
            description = "Temps de reflexion moyen, nombre de tentatives, score de reformulation (cf. section 4.1)")
    public ResponseEntity<EleveSuiviResponse> monTableauDeBord(@AuthenticationPrincipal Eleve eleve) {
        return ResponseEntity.ok(tableauDeBordService.construireSuiviEleve(eleve.getId()));
    }

    @GetMapping("/progression")
    @Operation(summary = "Consulter ma progression par matiere",
            description = "Historique et progression par matiere (cf. section 4.2 - V2).")
    public ResponseEntity<List<ProgressionMatiereResponse>> maProgression(@AuthenticationPrincipal Eleve eleve) {
        return ResponseEntity.ok(tableauDeBordService.construireProgressionParMatiere(eleve.getId()));
    }

    @GetMapping("/badges")
    @Operation(summary = "Consulter mes badges obtenus")
    public ResponseEntity<List<BadgeResponse>> mesBadges(@AuthenticationPrincipal Eleve eleve) {
        return ResponseEntity.ok(badgeService.listerBadgesDeLeleve(eleve.getId()));
    }

    @GetMapping("/bibliotheque")
    @Operation(summary = "Parcourir la bibliotheque d'exercices types",
            description = "Filtrable par matiere et/ou niveau.")
    public ResponseEntity<List<ExerciceTypeResponse>> parcourirBibliotheque(
            @RequestParam(required = false) String matiere,
            @RequestParam(required = false) String niveau
    ) {
        return ResponseEntity.ok(exerciceTypeService.rechercher(matiere, niveau));
    }

    @GetMapping("/mes-donnees")
    @Operation(summary = "Exporter mes donnees personnelles",
            description = "Droit d'acces / portabilite des donnees (conformite protection des donnees).")
    public ResponseEntity<DonneesEleveExportResponse> exporterMesDonnees(@AuthenticationPrincipal Eleve eleve) {
        return ResponseEntity.ok(gdprService.exporterDonnees(eleve.getId()));
    }

    @DeleteMapping("/mon-compte")
    @Operation(summary = "Supprimer definitivement mon compte et mes donnees",
            description = "Droit a l'oubli. Operation irreversible : supprime le compte et toutes les donnees associees (exercices, tentatives, badges...).")
    public ResponseEntity<Void> supprimerMonCompte(@AuthenticationPrincipal Eleve eleve) {
        gdprService.supprimerCompteEtDonnees(eleve.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quota-ia-restant")
    @Operation(summary = "Consulter mon quota de requetes IA restant pour aujourd'hui",
            description = "Chaque tentative, deblocage d'indice ou reformulation consomme une requete du quota quotidien.")
    public ResponseEntity<Integer> quotaRestant(@AuthenticationPrincipal Eleve eleve) {
        return ResponseEntity.ok(quotaIaService.consulterRequetesRestantes(eleve.getId()));
    }

    @GetMapping("/code-liaison-parent")
    @Operation(summary = "Generer un code pour rattacher un parent a mon compte",
            description = "A partager avec son parent (expire au bout de 30 minutes). Le lien se fait toujours "
                    + "a l'initiative de l'eleve : cela evite qu'un inconnu se rattache a ce compte juste en "
                    + "connaissant son adresse email.")
    public ResponseEntity<CodeLiaisonResponse> genererCodeLiaisonParent(@AuthenticationPrincipal Eleve eleve) {
        return ResponseEntity.ok(liaisonParentEleveService.genererCode(eleve.getId()));
    }
}
