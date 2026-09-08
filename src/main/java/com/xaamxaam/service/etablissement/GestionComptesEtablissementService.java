package com.xaamxaam.service.etablissement;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.AdminEtablissement;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Enseignant;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.dto.request.CreerEleveParAdminRequest;
import com.xaamxaam.dto.request.CreerEnseignantParAdminRequest;
import com.xaamxaam.dto.response.CompteEleveResponse;
import com.xaamxaam.dto.response.CompteEnseignantResponse;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.EnseignantRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des comptes eleves et enseignants par l'administrateur de son
 * propre etablissement (cf. section 12 du cahier des charges : "gestion des
 * inscriptions" deleguee au niveau etablissement une fois celui-ci valide
 * par le superadmin).
 *
 * Regle de securite centrale de cette classe : un AdminEtablissement ne
 * peut jamais agir en dehors de son propre etablissement. L'etablissement
 * n'est JAMAIS pris depuis les parametres de la requete cote client, mais
 * toujours depuis le compte AdminEtablissement authentifie
 * (@AuthenticationPrincipal cote controller), pour eviter qu'un admin
 * modifie les comptes d'un autre etablissement en changeant un ID dans le
 * corps de la requete.
 */
@Service
@RequiredArgsConstructor
public class GestionComptesEtablissementService {

    private final EleveRepository eleveRepository;
    private final EnseignantRepository enseignantRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CompteEleveResponse creerEleve(AdminEtablissement admin, CreerEleveParAdminRequest request) {
        Etablissement etablissement = exigerEtablissement(admin);
        verifierEmailDisponible(request.email());

        Eleve eleve = Eleve.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .motDePasse(passwordEncoder.encode(request.motDePasse()))
                .role(Role.ELEVE)
                .niveauScolaire(Eleve.NiveauScolaire.depuisTexte(request.niveauScolaire()))
                .etablissement(etablissement)
                // Cree par l'admin de l'etablissement : compte actif immediatement,
                // pas besoin de repasser par la validation manuelle du superadmin.
                .actif(true)
                .build();

        return versReponse(eleveRepository.save(eleve));
    }

    @Transactional
    public CompteEnseignantResponse creerEnseignant(AdminEtablissement admin, CreerEnseignantParAdminRequest request) {
        Etablissement etablissement = exigerEtablissement(admin);
        verifierEmailDisponible(request.email());

        Enseignant enseignant = Enseignant.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .motDePasse(passwordEncoder.encode(request.motDePasse()))
                .role(Role.ENSEIGNANT)
                .matiere(request.matiere())
                .etablissement(etablissement)
                .actif(true)
                .build();

        return versReponse(enseignantRepository.save(enseignant));
    }

    public List<CompteEleveResponse> listerEleves(AdminEtablissement admin) {
        Etablissement etablissement = exigerEtablissement(admin);
        return eleveRepository.findByEtablissement(etablissement).stream()
                .map(this::versReponse)
                .toList();
    }

    public List<CompteEnseignantResponse> listerEnseignants(AdminEtablissement admin) {
        // findByEtablissement n'existe pas encore sur EnseignantRepository dans le MVP ;
        // filtrage applicatif simple, acceptable au volume d'un seul etablissement.
        Etablissement etablissement = exigerEtablissement(admin);
        return enseignantRepository.findAll().stream()
                .filter(e -> e.getEtablissement() != null && e.getEtablissement().getId().equals(etablissement.getId()))
                .map(this::versReponse)
                .toList();
    }

    @Transactional
    public void basculerActivationEleve(AdminEtablissement admin, UUID eleveId, boolean actif) {
        Etablissement etablissement = exigerEtablissement(admin);
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));
        verifierAppartientAEtablissement(eleve.getEtablissement(), etablissement);
        eleve.setActif(actif);
        eleveRepository.save(eleve);
    }

    @Transactional
    public void basculerActivationEnseignant(AdminEtablissement admin, UUID enseignantId, boolean actif) {
        Etablissement etablissement = exigerEtablissement(admin);
        Enseignant enseignant = enseignantRepository.findById(enseignantId)
                .orElseThrow(() -> new ResourceNotFoundException("Enseignant introuvable"));
        verifierAppartientAEtablissement(enseignant.getEtablissement(), etablissement);
        enseignant.setActif(actif);
        enseignantRepository.save(enseignant);
    }

    private Etablissement exigerEtablissement(AdminEtablissement admin) {
        if (admin.getEtablissement() == null) {
            throw new IllegalStateException("Ce compte administrateur n'est rattache a aucun etablissement.");
        }
        return admin.getEtablissement();
    }

    private void verifierAppartientAEtablissement(Etablissement etablissementDuCompte, Etablissement etablissementDeLAdmin) {
        if (etablissementDuCompte == null || !etablissementDuCompte.getId().equals(etablissementDeLAdmin.getId())) {
            throw new AccesRefuseException("Ce compte n'appartient pas a ton etablissement.");
        }
    }

    private void verifierEmailDisponible(String email) {
        if (utilisateurRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Un compte existe deja avec cet email.");
        }
    }

    private CompteEleveResponse versReponse(Eleve eleve) {
        return new CompteEleveResponse(
                eleve.getId(), eleve.getNom(), eleve.getPrenom(), eleve.getEmail(),
                eleve.getNiveauScolaire(), eleve.isActif()
        );
    }

    private CompteEnseignantResponse versReponse(Enseignant enseignant) {
        return new CompteEnseignantResponse(
                enseignant.getId(), enseignant.getNom(), enseignant.getPrenom(), enseignant.getEmail(),
                enseignant.getMatiere(), enseignant.isActif()
        );
    }
}
