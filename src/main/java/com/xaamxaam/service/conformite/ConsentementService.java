package com.xaamxaam.service.conformite;

import com.xaamxaam.domain.conformite.ConsentementParental;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Parent;
import com.xaamxaam.dto.request.ConsentementRequest;
import com.xaamxaam.dto.response.ConsentementResponse;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.ConsentementParentalRepository;
import com.xaamxaam.repository.EleveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gere le consentement parental pour un eleve mineur : traitement general
 * des donnees, et autorisation separee pour que l'etablissement voie des
 * statistiques NOMINATIVES plutot qu'uniquement agregees (cf. section 4.2
 * du cahier des charges).
 */
@Service
@RequiredArgsConstructor
public class ConsentementService {

    private final ConsentementParentalRepository consentementRepository;
    private final EleveRepository eleveRepository;

    @Transactional
    public ConsentementResponse mettreAJourConsentement(Parent parent, UUID eleveId, ConsentementRequest request) {
        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new ResourceNotFoundException("Eleve introuvable"));

        ConsentementParental consentement = consentementRepository.findByEleveId(eleveId)
                .orElseGet(() -> ConsentementParental.builder().eleve(eleve).build());

        LocalDateTime maintenant = LocalDateTime.now();
        consentement.setParent(parent);

        if (request.consentementDonnees() != consentement.isConsentementDonnees()) {
            consentement.setConsentementDonnees(request.consentementDonnees());
            consentement.setDateConsentementDonnees(maintenant);
        }
        if (request.consentementStatistiquesNominatives() != consentement.isConsentementStatistiquesNominatives()) {
            consentement.setConsentementStatistiquesNominatives(request.consentementStatistiquesNominatives());
            consentement.setDateConsentementStatistiques(maintenant);
        }
        consentement.setDateMaj(maintenant);

        ConsentementParental sauvegarde = consentementRepository.save(consentement);
        return versReponse(sauvegarde);
    }

    public ConsentementResponse consulterConsentement(UUID eleveId) {
        return consentementRepository.findByEleveId(eleveId)
                .map(this::versReponse)
                .orElse(new ConsentementResponse(eleveId, false, null, false, null));
    }

    /** Utilise par StatistiquesService pour savoir si un eleve peut apparaitre nominativement. */
    public boolean aConsentiAuxStatistiquesNominatives(UUID eleveId) {
        return consentementRepository.findByEleveId(eleveId)
                .map(ConsentementParental::isConsentementStatistiquesNominatives)
                .orElse(false);
    }

    private ConsentementResponse versReponse(ConsentementParental c) {
        return new ConsentementResponse(
                c.getEleve().getId(),
                c.isConsentementDonnees(),
                c.getDateConsentementDonnees(),
                c.isConsentementStatistiquesNominatives(),
                c.getDateConsentementStatistiques()
        );
    }
}
