package com.xaamxaam.seed;

import com.xaamxaam.domain.abonnement.Abonnement;
import com.xaamxaam.domain.conformite.ConsentementParental;
import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.exercice.*;
import com.xaamxaam.domain.user.*;
import com.xaamxaam.repository.*;
import com.xaamxaam.service.exercice.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Peuple la base avec un jeu de donnees de test complet et coherent :
 * etablissements, un compte par role, exercices dans differents etats
 * (en cours / termine avec score), badges, consentements, abonnements.
 *
 * N'est JAMAIS actif par defaut - uniquement sous le profil "seed", pour
 * eviter tout risque d'executer ce code en production :
 *
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *   (ou SPRING_PROFILES_ACTIVE=seed si lance depuis un jar/Docker)
 *
 * Idempotent : si le superadmin de seed existe deja, la methode s'arrete
 * immediatement (log d'info) plutot que de dupliquer les donnees a chaque
 * redemarrage.
 *
 * IMPORTANT : ce runner n'appelle JAMAIS SocratiqueService/ClaudeApiClient
 * (donc ne necessite PAS ANTHROPIC_API_KEY) - les tentatives, indices et
 * reformulations sont inseres directement avec un contenu factice mais
 * plausible, en construisant les entites a la main plutot qu'en passant
 * par ExerciceService.
 */
@Component
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class SeedDataRunner implements CommandLineRunner {

    private static final String MOT_DE_PASSE_PAR_DEFAUT = "Password123!";
    private static final String EMAIL_SUPERADMIN = "superadmin@xaamxaam.sn";

    private final PasswordEncoder passwordEncoder;

    private final SuperadminRepository superadminRepository;
    private final EtablissementRepository etablissementRepository;
    private final AdminEtablissementRepository adminEtablissementRepository;
    private final EnseignantRepository enseignantRepository;
    private final EleveRepository eleveRepository;
    private final ParentRepository parentRepository;
    private final ExerciceRepository exerciceRepository;
    private final TentativeRepository tentativeRepository;
    private final IndiceRepository indiceRepository;
    private final ReformulationRepository reformulationRepository;
    private final ExerciceTypeRepository exerciceTypeRepository;
    private final ConsentementParentalRepository consentementParentalRepository;
    private final AbonnementRepository abonnementRepository;
    private final SignalementContournementRepository signalementContournementRepository;
    private final BadgeService badgeService;

    @Override
    @Transactional
    public void run(String... args) {
        if (superadminRepository.findAll().stream().anyMatch(s -> s.getEmail().equals(EMAIL_SUPERADMIN))) {
            log.info("Seed ignore : des donnees de test existent deja (superadmin '{}' trouve).", EMAIL_SUPERADMIN);
            return;
        }

        log.info("=== Debut du seed de donnees de test ===");

        String motDePasseHache = passwordEncoder.encode(MOT_DE_PASSE_PAR_DEFAUT);

        // ===== Superadmin =====
        Superadmin superadmin = Superadmin.builder()
                .nom("Ba").prenom("Cheikh").email(EMAIL_SUPERADMIN)
                .motDePasse(motDePasseHache).role(Role.SUPERADMIN).actif(true).emailVerifie(true)
                .build();
        superadminRepository.save(superadmin);

        // ===== Etablissements (deja valides, licence active) =====
        Etablissement lycee = etablissementRepository.save(Etablissement.builder()
                .nom("Lycée Seydou Nourou Tall").type(Etablissement.TypeEtablissement.LYCEE)
                .adresse("Dakar, Sénégal").licenceActive(true).enAttenteValidation(false)
                .dateExpirationLicence(LocalDate.now().plusYears(1))
                .build());

        Etablissement college = etablissementRepository.save(Etablissement.builder()
                .nom("Collège Sacré-Cœur").type(Etablissement.TypeEtablissement.COLLEGE)
                .adresse("Thiès, Sénégal").licenceActive(true).enAttenteValidation(false)
                .dateExpirationLicence(LocalDate.now().plusYears(1))
                .build());

        // ===== Administrateurs d'etablissement =====
        AdminEtablissement adminLycee = adminEtablissementRepository.save(AdminEtablissement.builder()
                .nom("Fall").prenom("Moussa").email("admin.lycee@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ADMIN_ETABLISSEMENT).actif(true).emailVerifie(true)
                .etablissement(lycee)
                .build());

        adminEtablissementRepository.save(AdminEtablissement.builder()
                .nom("Sy").prenom("Aminata").email("admin.college@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ADMIN_ETABLISSEMENT).actif(true).emailVerifie(true)
                .etablissement(college)
                .build());
        log.info("Admin établissement créé : {}", adminLycee.getEmail());

        // ===== Enseignants =====
        Enseignant profMaths = enseignantRepository.save(Enseignant.builder()
                .nom("Sarr").prenom("Ousmane").email("prof.maths@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ENSEIGNANT).actif(true).emailVerifie(true)
                .matiere("Mathématiques").etablissement(lycee)
                .build());

        Enseignant profPhysique = enseignantRepository.save(Enseignant.builder()
                .nom("Diagne").prenom("Bineta").email("prof.physique@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ENSEIGNANT).actif(true).emailVerifie(true)
                .matiere("Physique-Chimie").etablissement(lycee)
                .build());

        Enseignant profFrancais = enseignantRepository.save(Enseignant.builder()
                .nom("Kane").prenom("Ramatoulaye").email("prof.francais@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ENSEIGNANT).actif(true).emailVerifie(true)
                .matiere("Français").etablissement(college)
                .build());

        // ===== Eleves =====
        Eleve seydou = eleveRepository.save(Eleve.builder()
                .nom("Diop").prenom("Seydou").email("eleve.seydou@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ELEVE).actif(true).emailVerifie(true)
                .niveauScolaire(Eleve.NiveauScolaire.TROISIEME).etablissement(college)
                .build());

        Eleve awa = eleveRepository.save(Eleve.builder()
                .nom("Ndiaye").prenom("Awa").email("eleve.awa@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ELEVE).actif(true).emailVerifie(true)
                .niveauScolaire(Eleve.NiveauScolaire.SECONDE).etablissement(lycee)
                .build());

        Eleve moussa = eleveRepository.save(Eleve.builder()
                .nom("Sow").prenom("Moussa").email("eleve.moussa@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ELEVE).actif(true).emailVerifie(true)
                .niveauScolaire(Eleve.NiveauScolaire.TERMINALE).etablissement(lycee)
                .build());

        Eleve fatou = eleveRepository.save(Eleve.builder()
                .nom("Gueye").prenom("Fatou").email("eleve.fatou@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ELEVE).actif(true).emailVerifie(true)
                .niveauScolaire(Eleve.NiveauScolaire.PREMIERE).etablissement(lycee)
                .build());

        Eleve ibrahima = eleveRepository.save(Eleve.builder()
                .nom("Diallo").prenom("Ibrahima").email("eleve.ibrahima@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ELEVE).actif(true).emailVerifie(true)
                .niveauScolaire(Eleve.NiveauScolaire.QUATRIEME).etablissement(college)
                .build());

        Eleve mariama = eleveRepository.save(Eleve.builder()
                .nom("Cissé").prenom("Mariama").email("eleve.mariama@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.ELEVE).actif(true).emailVerifie(true)
                .niveauScolaire(Eleve.NiveauScolaire.LICENCE_1).etablissement(null)
                .build());

        // ===== Parents, rattaches directement (le flux normal passe par
        // un code genere par l'eleve - voir LiaisonParentEleveService -
        // ici on ecrit directement la relation pour le seed) =====
        Parent parentDiop = Parent.builder()
                .nom("Diop").prenom("Cheikh").email("parent.diop@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.PARENT).actif(true).emailVerifie(true)
                .build();
        parentDiop.getEleves().add(seydou);
        parentDiop.getEleves().add(ibrahima);
        parentRepository.save(parentDiop);

        Parent parentNdiaye = Parent.builder()
                .nom("Ndiaye").prenom("Astou").email("parent.ndiaye@xaamxaam.sn")
                .motDePasse(motDePasseHache).role(Role.PARENT).actif(true).emailVerifie(true)
                .build();
        parentNdiaye.getEleves().add(awa);
        parentRepository.save(parentNdiaye);

        log.info("Parents créés et rattachés : {}, {}", parentDiop.getEmail(), parentNdiaye.getEmail());

        // ===== Consentements parentaux =====
        LocalDateTime maintenant = LocalDateTime.now();
        consentementParentalRepository.save(ConsentementParental.builder()
                .eleve(seydou).parent(parentDiop)
                .consentementDonnees(true).dateConsentementDonnees(maintenant)
                .consentementStatistiquesNominatives(true).dateConsentementStatistiques(maintenant)
                .build());
        consentementParentalRepository.save(ConsentementParental.builder()
                .eleve(ibrahima).parent(parentDiop)
                .consentementDonnees(true).dateConsentementDonnees(maintenant)
                .consentementStatistiquesNominatives(false)
                .build());
        consentementParentalRepository.save(ConsentementParental.builder()
                .eleve(awa).parent(parentNdiaye)
                .consentementDonnees(true).dateConsentementDonnees(maintenant)
                .consentementStatistiquesNominatives(true).dateConsentementStatistiques(maintenant)
                .build());

        // ===== Bibliotheque d'exercices types =====
        exerciceTypeRepository.save(ExerciceType.builder()
                .titre("Équation du second degré")
                .enonce("Résoudre l'équation x² - 5x + 6 = 0")
                .matiere("Mathématiques").niveau("SECONDE")
                .creePar(profMaths).etablissement(lycee)
                .build());
        exerciceTypeRepository.save(ExerciceType.builder()
                .titre("Vitesse moyenne")
                .enonce("Un mobile parcourt 150 mètres en 30 secondes. Calculer sa vitesse moyenne en m/s puis en km/h.")
                .matiere("Physique-Chimie").niveau("TERMINALE")
                .creePar(profPhysique).etablissement(lycee)
                .build());
        exerciceTypeRepository.save(ExerciceType.builder()
                .titre("Figures de style")
                .enonce("Identifier et expliquer la figure de style dans la phrase : « Le vent hurlait dans les arbres. »")
                .matiere("Français").niveau("QUATRIEME")
                .creePar(profFrancais).etablissement(college)
                .build());

        // ===== Exercices =====
        creerExerciceTermine(seydou, "Mathématiques", "TROISIEME",
                "Résoudre l'équation x² - 4 = 0",
                "Je pense qu'il faut factoriser l'expression.",
                "Bonne piste ! Comment peux-tu écrire x² - 4 sous forme d'un produit de deux facteurs ?",
                "J'ai reconnu une identité remarquable (a²-b²), factorisé en (x-2)(x+2), puis trouvé x=2 et x=-2.",
                85, "Très bonne utilisation de l'identité remarquable, raisonnement clair.");

        creerExerciceTermine(seydou, "Français", "TROISIEME",
                "Analyser la figure de style utilisée dans : « Cette nouvelle m'a frappé comme la foudre. »",
                "Je crois que c'est une comparaison.",
                "Exactement ! Quels sont les deux éléments comparés, et quel mot introduit la comparaison ?",
                "C'est une comparaison entre la nouvelle et la foudre, introduite par 'comme', pour montrer la soudaineté et l'intensité du choc.",
                78, "Bonne identification, l'explication de l'effet recherché pourrait être un peu plus développée.");

        creerExerciceEnCours(seydou, "Mathématiques", "TROISIEME",
                "Développer et réduire l'expression (2x+3)(x-1)",
                "Je multiplie chaque terme du premier parenthèse par chaque terme du second ?",
                "C'est exactement la bonne méthode ! Vas-y, applique-la étape par étape.");

        creerExerciceTermine(awa, "Mathématiques", "SECONDE",
                "Étudier le signe de f(x) = 2x - 6",
                "f(x) = 0 quand x = 3 je crois.",
                "Bien vu ! Maintenant, comment le signe de f(x) évolue-t-il avant et après x=3 ?",
                "f(x) est négative avant x=3 et positive après, car le coefficient devant x est positif.",
                92, "Excellente maîtrise du signe d'une fonction affine.");

        creerExerciceTermine(awa, "Physique-Chimie", "SECONDE",
                "Calculer la masse volumique d'un objet de masse 270g et de volume 100cm³",
                "Je divise la masse par le volume ?",
                "Oui ! Fais bien attention aux unités avant de calculer.",
                "J'ai converti en grammes et cm³, puis divisé : 270/100 = 2.7 g/cm³.",
                65, "Résultat correct, mais la vérification des unités méritait plus de rigueur au départ.");

        creerExerciceEnCours(awa, "Mathématiques", "SECONDE",
                "Résoudre l'inéquation 3x - 5 > 1",
                "J'ajoute 5 des deux côtés ?",
                "Bonne première étape ! Que fais-tu ensuite pour isoler x ?");

        // Moussa : 5 exercices termines avec bon score -> declenche
        // PREMIER_PAS, APPRENTI et MAITRE_REFORMULATION (voir BadgeService)
        creerExerciceTermine(moussa, "Mathématiques", "TERMINALE",
                "Calculer la dérivée de f(x) = 3x² - 2x + 5",
                "f'(x) = 6x - 2 ?",
                "Exactement ! Peux-tu expliquer la règle que tu as utilisée ?",
                "J'ai appliqué la règle de dérivation des puissances terme par terme.",
                88, "Maîtrise solide des règles de dérivation.");
        creerExerciceTermine(moussa, "Physique-Chimie", "TERMINALE",
                "Déterminer l'énergie cinétique d'un objet de 2kg à 10m/s",
                "Ec = 1/2 * m * v² donc 100 joules ?",
                "Bravo, la formule est bien appliquée ! Vérifie juste l'unité finale.",
                "Ec = 0.5 * 2 * 100 = 100 joules, l'unité est bien le joule.",
                90, "Application rigoureuse de la formule, bonne vérification des unités.");
        creerExerciceTermine(moussa, "Mathématiques", "TERMINALE",
                "Étudier la limite de f(x) = 1/x quand x tend vers +∞",
                "Je pense que la limite est 0.",
                "Bonne intuition ! Comment le justifies-tu rigoureusement ?",
                "Plus x est grand, plus 1/x se rapproche de 0, donc la limite est 0.",
                82, "Bon raisonnement intuitif, une formalisation plus rigoureuse serait un plus.");
        creerExerciceTermine(moussa, "Physique-Chimie", "TERMINALE",
                "Calculer la résistance équivalente de deux résistances de 10Ω en série",
                "En série on additionne, donc 20Ω ?",
                "Parfait ! Et si elles étaient en parallèle, quelle formule utiliserais-tu ?",
                "En série R = R1+R2 = 20Ω, en parallèle j'utiliserais 1/R = 1/R1 + 1/R2.",
                95, "Excellente maîtrise des deux configurations, bien au-delà de la question posée.");
        creerExerciceTermine(moussa, "Mathématiques", "TERMINALE",
                "Résoudre l'équation différentielle y' = 2y",
                "Je pense que c'est une exponentielle.",
                "Bonne intuition ! Quelle est la forme générale des solutions ?",
                "Les solutions sont de la forme y = C*e^(2x), avec C une constante.",
                79, "Bonne reconnaissance de la forme générale, la justification pourrait être plus détaillée.");

        creerExerciceTermine(fatou, "Mathématiques", "PREMIERE",
                "Calculer le discriminant de x² + 2x - 3",
                "Δ = b² - 4ac donc Δ = 4 + 12 = 16 ?",
                "Exact ! Que peux-tu en déduire sur le nombre de solutions ?",
                "Comme Δ > 0, il y a deux solutions réelles distinctes.",
                70, "Calcul correct, conclusion bien amenée.");
        creerExerciceTermine(fatou, "Physique-Chimie", "PREMIERE",
                "Déterminer la concentration molaire d'une solution contenant 0.5 mol dans 2L",
                "C = n/V donc 0.25 mol/L ?",
                "C'est la bonne formule ! Vérifie ton calcul.",
                "0.5 divisé par 2 donne bien 0.25 mol/L.",
                75, "Bonne application directe de la formule.");
        creerExerciceEnCours(fatou, "Mathématiques", "PREMIERE",
                "Étudier la parité de la fonction f(x) = x³ - x",
                "Je calcule f(-x) ?",
                "Bonne méthode ! Que trouves-tu pour f(-x) ?");

        creerExerciceTermine(ibrahima, "Français", "QUATRIEME",
                "Identifier le temps et le mode du verbe dans : « Il aurait aimé venir. »",
                "Je pense que c'est du conditionnel.",
                "Bien vu ! Est-ce le présent ou le passé du conditionnel ?",
                "C'est le conditionnel passé, car il y a l'auxiliaire 'aurait' + participe passé.",
                60, "Identification correcte, l'explication de la formation pourrait être plus précise.");
        creerExerciceEnCours(ibrahima, "Mathématiques", "QUATRIEME",
                "Calculer le périmètre d'un rectangle de longueur 8cm et largeur 5cm",
                "P = 2*(L+l) donc 26cm ?",
                "Bonne formule ! Vérifie ton calcul étape par étape.");

        creerExerciceTermine(mariama, "Méthodologie", "LICENCE_1",
                "Construire un plan détaillé pour une dissertation sur la liberté",
                "Je pense faire une thèse/antithèse/synthèse classique.",
                "Bon point de départ ! Quelle serait ta thèse pour la première partie ?",
                "J'ai construit un plan en trois parties : la liberté comme absence de contrainte, ses limites nécessaires, puis une liberté responsable.",
                88, "Plan structuré et progressif, bonne maîtrise de l'exercice de dissertation.");
        creerExerciceEnCours(mariama, "Statistiques", "LICENCE_1",
                "Calculer la moyenne et l'écart-type de la série : 4, 8, 6, 5, 3",
                "La moyenne est 5.2, non ?",
                "Bon calcul ! Maintenant, comment calcules-tu l'écart-type à partir de cette moyenne ?");

        // ===== Badges (reutilise la vraie logique metier, aucune donnee inventee) =====
        badgeService.evaluerBadges(seydou.getId());
        badgeService.evaluerBadges(awa.getId());
        badgeService.evaluerBadges(moussa.getId());
        badgeService.evaluerBadges(fatou.getId());
        badgeService.evaluerBadges(ibrahima.getId());
        badgeService.evaluerBadges(mariama.getId());

        // ===== Abonnements =====
        abonnementRepository.save(Abonnement.builder()
                .type(Abonnement.TypeAbonnement.ETABLISSEMENT).etablissement(lycee)
                .dateDebut(LocalDate.now().minusMonths(2)).dateFin(LocalDate.now().plusYears(1))
                .statut(Abonnement.StatutAbonnement.ACTIF).montantFcfa(500_000L)
                .build());
        abonnementRepository.save(Abonnement.builder()
                .type(Abonnement.TypeAbonnement.PARENT).parent(parentDiop)
                .dateDebut(LocalDate.now().minusMonths(1))
                .statut(Abonnement.StatutAbonnement.ACTIF).montantFcfa(1_500L)
                .build());

        // ===== Un signalement de contournement pour demo (Superadmin) =====
        exerciceRepository.findByEleveIdOrderByDateCreationDesc(moussa.getId()).stream().findFirst()
                .ifPresent(exercice -> signalementContournementRepository.save(SignalementContournement.builder()
                        .eleve(moussa).exercice(exercice)
                        .type(SignalementContournement.TypeSignalement.RAPIDITE_SUSPECTE)
                        .details("Deux tentatives soumises en 2 secondes, potentiellement automatisé (donnée de démo).")
                        .traite(false)
                        .build()));

        log.info("=== Seed termine avec succes ===");
        log.info("Mot de passe commun a tous les comptes de test : {}", MOT_DE_PASSE_PAR_DEFAUT);
        log.info("Superadmin : {}", EMAIL_SUPERADMIN);
        log.info("Comptes eleves : eleve.seydou / eleve.awa / eleve.moussa / eleve.fatou / eleve.ibrahima / eleve.mariama @xaamxaam.sn");
        log.info("Comptes parents : parent.diop / parent.ndiaye @xaamxaam.sn");
        log.info("Comptes enseignants : prof.maths / prof.physique / prof.francais @xaamxaam.sn");
        log.info("Comptes admin etablissement : admin.lycee / admin.college @xaamxaam.sn");
    }

    private void creerExerciceTermine(
            Eleve eleve, String matiere, String niveau, String enonce,
            String tentativeTexte, String relanceIa,
            String reformulationTexte, int score, String feedbackIa
    ) {
        Exercice exercice = exerciceRepository.save(Exercice.builder()
                .eleve(eleve).enonce(enonce).matiere(matiere).niveau(niveau)
                .statut(Exercice.StatutExercice.EN_COURS)
                .build());

        creerIndicesVides(exercice);

        tentativeRepository.save(Tentative.builder()
                .exercice(exercice).contenu(tentativeTexte).relanceIa(relanceIa).indiceNiveau(0)
                .build());

        Reformulation reformulation = reformulationRepository.save(Reformulation.builder()
                .exercice(exercice).texte(reformulationTexte).score(score).feedbackIa(feedbackIa)
                .build());

        exercice.setReformulation(reformulation);
        exercice.setStatut(Exercice.StatutExercice.TERMINE);
        exerciceRepository.save(exercice);
    }

    private void creerExerciceEnCours(
            Eleve eleve, String matiere, String niveau, String enonce,
            String tentativeTexte, String relanceIa
    ) {
        Exercice exercice = exerciceRepository.save(Exercice.builder()
                .eleve(eleve).enonce(enonce).matiere(matiere).niveau(niveau)
                .statut(Exercice.StatutExercice.EN_COURS)
                .build());

        creerIndicesVides(exercice);

        tentativeRepository.save(Tentative.builder()
                .exercice(exercice).contenu(tentativeTexte).relanceIa(relanceIa).indiceNiveau(0)
                .build());
    }

    /** Reproduit le comportement de ExerciceService.creerExercice : 3 indices vides, non debloques. */
    private void creerIndicesVides(Exercice exercice) {
        for (int niveau = 1; niveau <= 3; niveau++) {
            indiceRepository.save(Indice.builder()
                    .exercice(exercice).niveau(niveau).contenu(null).debloque(false)
                    .build());
        }
    }
}
