package com.xaamxaam.controller;

import com.xaamxaam.domain.user.Enseignant;
import com.xaamxaam.dto.request.CreerExerciceTypeRequest;
import com.xaamxaam.dto.response.EleveStatNominativeResponse;
import com.xaamxaam.dto.response.ExerciceTypeResponse;
import com.xaamxaam.dto.response.StatistiquesClasseResponse;
import com.xaamxaam.service.exercice.ExerciceTypeService;
import com.xaamxaam.service.suivi.StatistiquesService;
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

@RestController
@RequestMapping("/api/enseignant")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Enseignant", description = "Statistiques agregees de classe, bibliotheque d'exercices, support de sensibilisation")
public class EnseignantController {

    private final StatistiquesService statistiquesService;
    private final ExerciceTypeService exerciceTypeService;

    @GetMapping("/statistiques")
    @Operation(summary = "Consulter les statistiques agregees de mon etablissement",
            description = "Vue agregee, non nominative par defaut (cf. section 4.2 du cahier des charges).")
    public ResponseEntity<StatistiquesClasseResponse> statistiques(@AuthenticationPrincipal Enseignant enseignant) {
        return ResponseEntity.ok(statistiquesService.construireStatistiquesEtablissement(
                enseignant.getEtablissement().getId()
        ));
    }

    @GetMapping("/statistiques/nominatives")
    @Operation(summary = "Consulter les statistiques nominatives des eleves ayant consenti",
            description = "Ne liste QUE les eleves dont le parent a explicitement autorise les statistiques "
                    + "nominatives (cf. ConsentementParental). Les autres n'apparaissent simplement pas ici.")
    public ResponseEntity<List<EleveStatNominativeResponse>> statistiquesNominatives(@AuthenticationPrincipal Enseignant enseignant) {
        return ResponseEntity.ok(statistiquesService.construireStatistiquesNominatives(
                enseignant.getEtablissement().getId()
        ));
    }

    @PostMapping("/bibliotheque")
    @Operation(summary = "Ajouter un exercice type a la bibliotheque")
    public ResponseEntity<ExerciceTypeResponse> ajouterExerciceType(
            @AuthenticationPrincipal Enseignant enseignant,
            @Valid @RequestBody CreerExerciceTypeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciceTypeService.creer(enseignant, request));
    }

    @GetMapping("/bibliotheque")
    @Operation(summary = "Parcourir la bibliotheque d'exercices types", description = "Filtrable par matiere et/ou niveau.")
    public ResponseEntity<List<ExerciceTypeResponse>> parcourirBibliotheque(
            @RequestParam(required = false) String matiere,
            @RequestParam(required = false) String niveau
    ) {
        return ResponseEntity.ok(exerciceTypeService.rechercher(matiere, niveau));
    }
}
