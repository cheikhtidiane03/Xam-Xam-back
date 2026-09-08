package com.xaamxaam.service.abonnement;

import com.xaamxaam.domain.abonnement.Abonnement;
import com.xaamxaam.repository.AbonnementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AbonnementService {

    private final AbonnementRepository abonnementRepository;

    public List<Abonnement> listerParParent(UUID parentId) {
        return abonnementRepository.findByParentId(parentId);
    }

    public List<Abonnement> listerParEtablissement(UUID etablissementId) {
        return abonnementRepository.findByEtablissementId(etablissementId);
    }

    @Transactional
    public Abonnement activer(UUID abonnementId) {
        Abonnement abonnement = abonnementRepository.findById(abonnementId)
                .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable"));
        abonnement.setStatut(Abonnement.StatutAbonnement.ACTIF);
        return abonnementRepository.save(abonnement);
    }
}
