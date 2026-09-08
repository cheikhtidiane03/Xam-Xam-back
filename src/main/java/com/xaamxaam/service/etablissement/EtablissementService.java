package com.xaamxaam.service.etablissement;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.dto.request.EtablissementRequest;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.EtablissementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EtablissementService {

    private final EtablissementRepository etablissementRepository;

    @Transactional
    public Etablissement creer(EtablissementRequest request) {
        Etablissement etablissement = Etablissement.builder()
                .nom(request.nom())
                .type(request.type())
                .adresse(request.adresse())
                .build();
        return etablissementRepository.save(etablissement);
    }

    public List<Etablissement> listerEnAttenteValidation() {
        return etablissementRepository.findByEnAttenteValidationTrue();
    }

    public List<Etablissement> listerTous() {
        return etablissementRepository.findAll();
    }

    @Transactional
    public Etablissement validerEtablissement(UUID etablissementId) {
        Etablissement etablissement = recuperer(etablissementId);
        etablissement.setEnAttenteValidation(false);
        etablissement.setLicenceActive(true);
        return etablissementRepository.save(etablissement);
    }

    @Transactional
    public void retirerEtablissement(UUID etablissementId) {
        Etablissement etablissement = recuperer(etablissementId);
        etablissementRepository.delete(etablissement);
    }

    public Etablissement recuperer(UUID etablissementId) {
        return etablissementRepository.findById(etablissementId)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement introuvable"));
    }
}
