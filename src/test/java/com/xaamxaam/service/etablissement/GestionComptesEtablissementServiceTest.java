package com.xaamxaam.service.etablissement;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.AdminEtablissement;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Enseignant;
import com.xaamxaam.dto.request.CreerEleveParAdminRequest;
import com.xaamxaam.dto.response.CompteEleveResponse;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.EnseignantRepository;
import com.xaamxaam.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ces tests verifient specifiquement la regle de securite centrale du
 * service : un administrateur d'etablissement ne peut jamais agir sur des
 * comptes rattaches a un AUTRE etablissement que le sien.
 */
@ExtendWith(MockitoExtension.class)
class GestionComptesEtablissementServiceTest {

    @Mock private EleveRepository eleveRepository;
    @Mock private EnseignantRepository enseignantRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private GestionComptesEtablissementService service;

    private Etablissement etablissementDeLAdmin;
    private Etablissement autreEtablissement;
    private AdminEtablissement admin;

    @BeforeEach
    void setUp() {
        service = new GestionComptesEtablissementService(eleveRepository, enseignantRepository, utilisateurRepository, passwordEncoder);

        etablissementDeLAdmin = Etablissement.builder()
                .id(UUID.randomUUID()).nom("Lycee Blaise Diagne").type(Etablissement.TypeEtablissement.LYCEE).build();
        autreEtablissement = Etablissement.builder()
                .id(UUID.randomUUID()).nom("College Sacre Coeur").type(Etablissement.TypeEtablissement.COLLEGE).build();

        admin = AdminEtablissement.builder()
                .id(UUID.randomUUID()).nom("Fall").prenom("Moussa").email("moussa@exemple.com")
                .motDePasse("hash").etablissement(etablissementDeLAdmin).build();
    }

    @Test
    void creerEleve_rattacheAutomatiquementLeleveALetablissementDeLAdmin() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-encode");
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(inv -> inv.getArgument(0));

        CreerEleveParAdminRequest request = new CreerEleveParAdminRequest(
                "Diop", "Awa", "awa@exemple.com", "motdepasse123", "TROISIEME"
        );

        CompteEleveResponse resultat = service.creerEleve(admin, request);

        assertThat(resultat.actif()).isTrue();
        verify(eleveRepository).save(argThat(eleve ->
                eleve.getEtablissement().getId().equals(etablissementDeLAdmin.getId())
        ));
    }

    @Test
    void creerEleve_refuseSiLemailExisteDeja() {
        when(utilisateurRepository.existsByEmail(anyString())).thenReturn(true);

        CreerEleveParAdminRequest request = new CreerEleveParAdminRequest(
                "Diop", "Awa", "awa@exemple.com", "motdepasse123", "TROISIEME"
        );

        assertThatThrownBy(() -> service.creerEleve(admin, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eleveRepository, never()).save(any());
    }

    @Test
    void basculerActivationEleve_refuseSiLeleveAppartientAUnAutreEtablissement() {
        Eleve eleveDeLAutreEtablissement = Eleve.builder()
                .id(UUID.randomUUID()).nom("Ndiaye").prenom("Fatou").email("fatou@exemple.com")
                .motDePasse("hash").etablissement(autreEtablissement)
                .niveauScolaire(Eleve.NiveauScolaire.SECONDE).build();

        when(eleveRepository.findById(eleveDeLAutreEtablissement.getId()))
                .thenReturn(Optional.of(eleveDeLAutreEtablissement));

        assertThatThrownBy(() -> service.basculerActivationEleve(admin, eleveDeLAutreEtablissement.getId(), false))
                .isInstanceOf(AccesRefuseException.class);

        verify(eleveRepository, never()).save(any());
    }

    @Test
    void basculerActivationEleve_reussitSiLeleveAppartientAuMemeEtablissement() {
        Eleve eleveDuMemeEtablissement = Eleve.builder()
                .id(UUID.randomUUID()).nom("Ndiaye").prenom("Fatou").email("fatou@exemple.com")
                .motDePasse("hash").etablissement(etablissementDeLAdmin).actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.SECONDE).build();

        when(eleveRepository.findById(eleveDuMemeEtablissement.getId()))
                .thenReturn(Optional.of(eleveDuMemeEtablissement));
        when(eleveRepository.save(any(Eleve.class))).thenAnswer(inv -> inv.getArgument(0));

        service.basculerActivationEleve(admin, eleveDuMemeEtablissement.getId(), false);

        assertThat(eleveDuMemeEtablissement.isActif()).isFalse();
        verify(eleveRepository).save(eleveDuMemeEtablissement);
    }

    @Test
    void basculerActivationEnseignant_refuseSiLenseignantAppartientAUnAutreEtablissement() {
        Enseignant enseignantDeLAutreEtablissement = Enseignant.builder()
                .id(UUID.randomUUID()).nom("Sow").prenom("Ibrahima").email("ibrahima@exemple.com")
                .motDePasse("hash").etablissement(autreEtablissement).matiere("Physique").build();

        when(enseignantRepository.findById(enseignantDeLAutreEtablissement.getId()))
                .thenReturn(Optional.of(enseignantDeLAutreEtablissement));

        assertThatThrownBy(() -> service.basculerActivationEnseignant(admin, enseignantDeLAutreEtablissement.getId(), true))
                .isInstanceOf(AccesRefuseException.class);
    }

    @Test
    void exigerEtablissement_leveIllegalStateSiLadminNestRattacheAAucunEtablissement() {
        AdminEtablissement adminSansEtablissement = AdminEtablissement.builder()
                .id(UUID.randomUUID()).nom("Sans").prenom("Etablissement").email("x@exemple.com")
                .motDePasse("hash").etablissement(null).build();

        CreerEleveParAdminRequest request = new CreerEleveParAdminRequest(
                "Diop", "Awa", "awa@exemple.com", "motdepasse123", "TROISIEME"
        );

        assertThatThrownBy(() -> service.creerEleve(adminSansEtablissement, request))
                .isInstanceOf(IllegalStateException.class);
    }
}
