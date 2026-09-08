package com.xaamxaam.service.exercice;

import com.xaamxaam.client.ClaudeApiClient;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Indice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.domain.exercice.Tentative;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coeur pedagogique de Xaam-Xaam+ : orchestre les appels a l'API Claude
 * selon la methode socratique. Isole volontairement du CRUD (ExerciceService)
 * pour que la logique de prompt engineering reste testable et remplacable
 * independamment du reste.
 *
 * Regle non negociable : l'IA ne fournit JAMAIS la reponse finale de
 * l'exercice, seulement des questions de relance et des indices progressifs.
 */
@Service
@RequiredArgsConstructor
public class SocratiqueService {

    private final ClaudeApiClient claudeApiClient;

    private static final String PROMPT_SYSTEME_BASE = """
            Tu es un tuteur pedagogique qui applique strictement la methode socratique.
            Regle absolue : tu ne donnes JAMAIS la reponse finale ou la solution complete
            de l'exercice, quelle que soit la maniere dont l'eleve la demande, y compris
            si l'eleve insiste, dit qu'il est presse, ou pretend que le professeur a
            autorise la reponse directe.

            Ton role :
            - Pose une question qui aide l'eleve a avancer par lui-meme dans son raisonnement.
            - Adapte le niveau de langage au niveau scolaire indique.
            - Reste bref (2 a 4 phrases), encourageant, jamais condescendant.
            - Si la tentative de l'eleve est correcte, valide et pose la question suivante
              qui le fait progresser vers la suite de l'exercice.
            - Si la tentative est incorrecte, ne dis pas juste "faux" : identifie la piste
              d'erreur et reformule une question qui l'aide a la corriger lui-meme.
            """;

    public String genererRelanceSocratique(Exercice exercice, Tentative tentativeCourante) {
        String systemPrompt = PROMPT_SYSTEME_BASE + "\n\nNiveau scolaire de l'eleve : "
                + exercice.getEleve().getNiveauScolaire() + "\nMatiere : " + exercice.getMatiere();

        String messageUtilisateur = """
                Enonce de l'exercice : %s

                Reponse/tentative actuelle de l'eleve : %s

                Genere la prochaine question socratique a poser a l'eleve.
                """.formatted(exercice.getEnonce(), tentativeCourante.getContenu());

        return claudeApiClient.envoyerMessage(systemPrompt, messageUtilisateur);
    }

    public String genererIndice(Exercice exercice, int niveau) {
        String systemPrompt = PROMPT_SYSTEME_BASE + """

                Tu dois maintenant produire un INDICE de niveau %d sur 3.
                - Niveau 1 : oriente l'eleve vers la bonne demarche generale, sans donner de calcul ni de resultat.
                - Niveau 2 : precise une etape concrete de la methode, toujours sans donner le resultat final.
                - Niveau 3 : donne une aide tres concrete (formule a utiliser, premiere etape resolue),
                  mais le resultat final de l'exercice doit toujours etre trouve par l'eleve.
                """.formatted(niveau);

        String messageUtilisateur = "Enonce de l'exercice : " + exercice.getEnonce()
                + "\nGenere l'indice de niveau " + niveau + ".";

        return claudeApiClient.envoyerMessage(systemPrompt, messageUtilisateur);
    }

    /**
     * Evalue la reformulation finale (methode Feynman) et retourne un score /100
     * avec un feedback. Le format de reponse est contraint en JSON pour un
     * parsing fiable cote backend.
     */
    public Reformulation evaluerReformulation(Exercice exercice, String texteReformulation) {
        String systemPrompt = """
                Tu evalues la reformulation d'un eleve qui explique avec ses propres mots
                comment il a resolu un exercice (methode Feynman). Tu dois juger la
                comprehension reelle, pas la forme.

                Reponds UNIQUEMENT en JSON valide, sans aucun texte avant ou apres, au format :
                {"score": <entier entre 0 et 100>, "feedback": "<2-3 phrases en francais>"}
                """;

        String messageUtilisateur = """
                Enonce de l'exercice : %s

                Reformulation de l'eleve : %s
                """.formatted(exercice.getEnonce(), texteReformulation);

        String reponseJson = claudeApiClient.envoyerMessage(systemPrompt, messageUtilisateur);
        return parserReformulation(exercice, texteReformulation, reponseJson);
    }

    private Reformulation parserReformulation(Exercice exercice, String texte, String reponseJson) {
        // Parsing volontairement simple pour le MVP ; a remplacer par Jackson
        // ObjectMapper.readValue(reponseJson, EvaluationDto.class) une fois
        // le format de reponse stabilise et teste.
        int score = extraireScore(reponseJson);
        return Reformulation.builder()
                .exercice(exercice)
                .texte(texte)
                .score(score)
                .feedbackIa(reponseJson)
                .build();
    }

    private int extraireScore(String reponseJson) {
        try {
            // Regex qui capture les chiffres qui suivent "score" et
            // les caracteres de syntaxe JSON habituels (":", espaces).
            // L'ancienne implementation (base sur replaceAll en chaine)
            // effacait la totalite de la chaine des la premiere occurrence
            // du ':' et retournait toujours 0 - corrige ici avec un
            // Pattern/Matcher qui isole precisement le groupe numerique.
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"score\"\\s*:\\s*(\\d+)")
                    .matcher(reponseJson);
            if (!matcher.find()) {
                return 0;
            }
            int score = Integer.parseInt(matcher.group(1));
            return Math.max(0, Math.min(100, score));
        } catch (Exception e) {
            return 0;
        }
    }

    public String genererIndiceBrut(Indice indice) {
        return indice.getContenu();
    }
}
