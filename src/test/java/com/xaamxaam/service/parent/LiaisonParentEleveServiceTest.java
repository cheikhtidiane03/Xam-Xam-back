package com.xaamxaam.service.parent;

import com.xaamxaam.domain.user.CodeLiaisonParentEleve;
import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Parent;
import com.xaamxaam.domain.user.Role;
import com.xaamxaam.exception.AccesRefuseException;
import com.xaamxaam.exception.TokenInvalideException;
import com.xaamxaam.repository.CodeLiaisonParentEleveRepository;
import com.xaamxaam.repository.EleveRepository;
import com.xaamxaam.repository.ParentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiaisonParentEleveServiceTest {

    @Mock private CodeLiaisonParentEleveRepository codeRepository;
    @Mock private EleveRepository eleveRepository;
    @Mock private ParentRepository parentRepository;

    private LiaisonParentEleveService service;

    @BeforeEach
    void setUp() {
        service = new LiaisonParentEleveService(codeRepository, eleveRepository, parentRepository);
    }

    private Eleve unEleve() {
        return Eleve.builder().id(UUID.randomUUID()).nom("Diop").prenom("Awa").email("awa@exemple.com")
                .motDePasse("hash").role(Role.ELEVE).actif(true)
                .niveauScolaire(Eleve.NiveauScolaire.TROISIEME).build();
    }

    private Parent unParent() {
        return Parent.builder().id(UUID.randomUUID()).nom("Ndiaye").prenom("Fatou").email("fatou@exemple.com")
                .motDePasse("hash").role(Role.PARENT).eleves(new HashSet<>()).build();
    }

    @Test
    void genererCode_creeUnCodeAvecExpirationDansLeFutur() {
        Eleve eleve = unEleve();
        when(eleveRepository.findById(eleve.getId())).thenReturn(Optional.of(eleve));

        var reponse = service.genererCode(eleve.getId());

        assertThat(reponse.code()).hasSize(8);
        assertThat(reponse.dateExpiration()).isAfter(LocalDateTime.now());
        verify(codeRepository).save(any(CodeLiaisonParentEleve.class));
    }

    @Test
    void relierAvecCode_refuseUnCodeInconnu() {
        Parent parent = unParent();
        when(codeRepository.findByCodeAndUtiliseFalse("ABCDEFGH")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.relierAvecCode(parent, "abcdefgh"))
                .isInstanceOf(TokenInvalideException.class);
    }

    @Test
    void relierAvecCode_refuseUnCodeExpire() {
        Parent parent = unParent();
        Eleve eleve = unEleve();
        CodeLiaisonParentEleve codeExpire = CodeLiaisonParentEleve.builder()
                .id(UUID.randomUUID()).eleve(eleve).code("ABCDEFGH")
                .dateExpiration(LocalDateTime.now().minusMinutes(1)).build();

        when(codeRepository.findByCodeAndUtiliseFalse("ABCDEFGH")).thenReturn(Optional.of(codeExpire));

        assertThatThrownBy(() -> service.relierAvecCode(parent, "ABCDEFGH"))
                .isInstanceOf(TokenInvalideException.class)
                .hasMessageContaining("expire");
    }

    @Test
    void relierAvecCode_rattacheLeleveEtInvalideLeCode() {
        Parent parentAuth = unParent();
        Parent parentGere = unParent();
        parentGere.setId(parentAuth.getId());
        Eleve eleve = unEleve();

        CodeLiaisonParentEleve code = CodeLiaisonParentEleve.builder()
                .id(UUID.randomUUID()).eleve(eleve).code("ABCDEFGH")
                .dateExpiration(LocalDateTime.now().plusMinutes(10)).build();

        when(codeRepository.findByCodeAndUtiliseFalse("ABCDEFGH")).thenReturn(Optional.of(code));
        when(parentRepository.findById(parentAuth.getId())).thenReturn(Optional.of(parentGere));

        service.relierAvecCode(parentAuth, "ABCDEFGH");

        assertThat(parentGere.getEleves()).contains(eleve);
        verify(parentRepository).save(parentGere);
        verify(codeRepository).save(argThat(CodeLiaisonParentEleve::isUtilise));
    }

    @Test
    void relierAvecCode_refuseSiLeleveEstDejaRattache() {
        Parent parentAuth = unParent();
        Eleve eleve = unEleve();
        Parent parentGere = unParent();
        parentGere.setId(parentAuth.getId());
        parentGere.getEleves().add(eleve);

        CodeLiaisonParentEleve code = CodeLiaisonParentEleve.builder()
                .id(UUID.randomUUID()).eleve(eleve).code("ABCDEFGH")
                .dateExpiration(LocalDateTime.now().plusMinutes(10)).build();

        when(codeRepository.findByCodeAndUtiliseFalse("ABCDEFGH")).thenReturn(Optional.of(code));
        when(parentRepository.findById(parentAuth.getId())).thenReturn(Optional.of(parentGere));

        assertThatThrownBy(() -> service.relierAvecCode(parentAuth, "ABCDEFGH"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(parentRepository, never()).save(any());
    }

    @Test
    void delierEnfant_leveAccesRefuseSiLeleveNestPasRattache() {
        Parent parentAuth = unParent();
        Parent parentGere = unParent();
        parentGere.setId(parentAuth.getId());

        when(parentRepository.findById(parentAuth.getId())).thenReturn(Optional.of(parentGere));

        assertThatThrownBy(() -> service.delierEnfant(parentAuth, UUID.randomUUID()))
                .isInstanceOf(AccesRefuseException.class);
    }

    @Test
    void delierEnfant_retireLeleveDeLaListe() {
        Parent parentAuth = unParent();
        Eleve eleve = unEleve();
        Parent parentGere = unParent();
        parentGere.setId(parentAuth.getId());
        parentGere.getEleves().add(eleve);

        when(parentRepository.findById(parentAuth.getId())).thenReturn(Optional.of(parentGere));

        service.delierEnfant(parentAuth, eleve.getId());

        assertThat(parentGere.getEleves()).doesNotContain(eleve);
        verify(parentRepository).save(parentGere);
    }
}
