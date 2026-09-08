package com.xaamxaam.service.exercice;

import com.xaamxaam.domain.exercice.SignalementContournement;
import com.xaamxaam.exception.ResourceNotFoundException;
import com.xaamxaam.repository.SignalementContournementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignalementContournementService {

    private final SignalementContournementRepository signalementRepository;

    public List<SignalementContournement> listerNonTraites() {
        return signalementRepository.findByTraiteFalseOrderByDateCreationDesc();
    }

    @Transactional
    public void marquerTraite(UUID signalementId) {
        SignalementContournement signalement = signalementRepository.findById(signalementId)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable"));
        signalement.setTraite(true);
        signalementRepository.save(signalement);
    }
}
