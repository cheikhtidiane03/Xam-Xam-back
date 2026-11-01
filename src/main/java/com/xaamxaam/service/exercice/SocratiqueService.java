package com.xaamxaam.service.exercice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xaamxaam.client.ClaudeApiClient;
import com.xaamxaam.domain.exercice.Exercice;
import com.xaamxaam.domain.exercice.Indice;
import com.xaamxaam.domain.exercice.Reformulation;
import com.xaamxaam.domain.exercice.Tentative;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coeur pedagogique de Xaam-Xaam+ : orchestre les appels a l'API Claude
 * selon la methode socratique. Isole volontairement du CRUD (ExerciceService)
 * pour que la logique de prompt engineering reste testable et remplacable
 * independamment du reste.
 *
 * Regle non negociable : l'IA ne fournit JAMAIS la reponse finale de
 * l'exercice, seulement des questions de relance et des indices progressifs.
 *
 * Langue : les relances, indices et le feedback final suivent la langue de
 * l'enonce de l'exercice (francais ou anglais).
 */
@Service
@RequiredArgsConstructor
public class SocratiqueService {

    private final ClaudeApiClient claudeApiClient;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> MOTS_FR = Set.of(
            "le", "la", "les", "un", "une", "des", "du", "de", "et", "est", "sont", "que", "qui",
            "pour", "dans", "avec", "sur", "quelle", "quel", "combien", "calculer", "calcule",
            "trouver", "montrer", "si", "on", "au", "aux", "ce", "cette", "ses", "son", "sa",
            "par", "pas", "il", "elle", "je", "nous", "vous", "mais", "ou", "donc", "apres");

    private static final Set<String> MOTS_EN = Set.of(
            "the", "a", "an", "of", "and", "is", "are", "that", "which", "for", "in", "with",
            "on", "what", "how", "calculate", "find", "show", "if", "it", "to", "at", "by",
            "its", "his", "her", "this", "from", "does", "do", "has", "have", "will", "long",
            "take", "i", "we", "you", "but", "or", "so", "then", "after", "was", "were");

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

    private enum Langue { FR, EN }

    /** Detecte la langue d'un texte (francais par defaut en cas d'egalite). */
    private static Langue detecterLangue(String... textes) {
        int fr = 0;
        int en = 0;
        for (String t : textes) {
            if (t == null) {
                continue;
            }
            for (String mot : t.toLowerCase().split("[^\\p{L}']+")) {
                if (MOTS_FR.contains(mot)) {
                    fr++;
                }
                if (MOTS_EN.contains(mot)) {
                    en++;
                }
            }
        }
        return en > fr ? Langue.EN : Langue.FR;
    }

    private static String regleLangue(Langue langue) {
        return langue == Langue.EN
                ? "\n\nLANGUAGE RULE: The student works in English. Write your ENTIRE reply in English, "
                  + "even though these instructions are in French."
                : "\n\nREGLE DE LANGUE : l'eleve travaille en francais. Reponds ENTIEREMENT en francais.";
    }

    public String genererRelanceSocratique(Exercice exercice, Tentative tentativeCourante) {
        Langue langue = detecterLangue(exercice.getEnonce(), tentativeCourante.getContenu());
        String systemPrompt = PROMPT_SYSTEME_BASE + "\n\nNiveau scolaire de l'eleve : "
                + exercice.getEleve().getNiveauScolaire() + "\nMatiere : " + exercice.getMatiere()
                + regleLangue(langue);

        String messageUtilisateur = """
                Enonce de l'exercice : %s

                Reponse/tentative actuelle de l'eleve : %s

                Genere la prochaine question socratique a poser a l'eleve.
                """.formatted(exercice.getEnonce(), tentativeCourante.getContenu());

        return claudeApiClient.envoyerMessage(systemPrompt, messageUtilisateur);
    }

    public String genererIndice(Exercice exercice, int niveau) {
        Langue langue = detecterLangue(exercice.getEnonce());
        String systemPrompt = PROMPT_SYSTEME_BASE + """

                Tu dois maintenant produire un INDICE de niveau %d sur 3.
                - Niveau 1 : oriente l'eleve vers la bonne demarche generale, sans donner de calcul ni de resultat.
                - Niveau 2 : precise une etape concrete de la methode, toujours sans donner le resultat final.
                - Niveau 3 : donne une aide tres concrete (formule a utiliser, premiere etape resolue),
                  mais le resultat final de l'exercice doit toujours etre trouve par l'eleve.
                """.formatted(niveau) + regleLangue(langue);

        String messageUtilisateur = "Enonce de l'exercice : " + exercice.getEnonce()
                + "\nGenere l'indice de niveau " + niveau + ".";

        return claudeApiClient.envoyerMessage(systemPrompt, messageUtilisateur);
    }

    /**
     * Evalue la reformulation finale (methode Feynman) et retourne un score /100
     * avec un feedback. Le format de reponse est contraint en JSON pour un
     * parsing fiable cote backend ; seul le texte du feedback est conserve.
     */
    public Reformulation evaluerReformulation(Exercice exercice, String texteReformulation) {
        Langue langue = detecterLangue(exercice.getEnonce(), texteReformulation);
        String nomLangue = langue == Langue.EN ? "English" : "francais";

        String systemPrompt = """
                Tu evalues la reformulation d'un eleve qui explique avec ses propres mots
                comment il a resolu un exercice (methode Feynman). Tu dois juger la
                comprehension reelle, pas la forme.

                Reponds UNIQUEMENT en JSON valide, sans aucun texte avant ou apres, sans bloc de code, au format :
                {"score": <entier entre 0 et 100>, "feedback": "<2-3 phrases en %s>"}
                Les cles "score" et "feedback" restent exactement ainsi ; seul le texte du feedback est en %s.
                """.formatted(nomLangue, nomLangue) + regleLangue(langue);

        String messageUtilisateur = """
                Enonce de l'exercice : %s

                Reformulation de l'eleve : %s
                """.formatted(exercice.getEnonce(), texteReformulation);

        String reponseJson = claudeApiClient.envoyerMessage(systemPrompt, messageUtilisateur);
        return parserReformulation(exercice, texteReformulation, reponseJson, langue);
    }

    private Reformulation parserReformulation(Exercice exercice, String texte, String reponse, Langue langue) {
        int score = 0;
        String feedback = null;
        try {
            JsonNode noeud = MAPPER.readTree(extraireJson(reponse));
            score = borner(noeud.path("score").asInt(0));
            feedback = noeud.path("feedback").asText(null);
        } catch (Exception e) {
            // Repli : extraction par regex si le JSON est mal forme.
            score = extraireScore(reponse);
            feedback = extraireFeedback(reponse);
        }
        if (feedback == null || feedback.isBlank()) {
            feedback = langue == Langue.EN
                    ? "Your explanation has been evaluated. Keep practising explaining in your own words."
                    : "Ton explication a ete evaluee. Continue a t'entrainer a expliquer avec tes propres mots.";
        }
        return Reformulation.builder()
                .exercice(exercice)
                .texte(texte)
                .score(score)
                .feedbackIa(feedback.trim())
                .build();
    }

    /** Isole l'objet JSON meme si Claude l'entoure de texte ou de ```json. */
    private static String extraireJson(String reponse) {
        if (reponse == null) {
            return "{}";
        }
        int debut = reponse.indexOf('{');
        int fin = reponse.lastIndexOf('}');
        return (debut >= 0 && fin > debut) ? reponse.substring(debut, fin + 1) : "{}";
    }

    private static int borner(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private int extraireScore(String reponse) {
        try {
            Matcher matcher = Pattern.compile("\"score\"\\s*:\\s*(\\d+)").matcher(reponse);
            return matcher.find() ? borner(Integer.parseInt(matcher.group(1))) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String extraireFeedback(String reponse) {
        try {
            Matcher matcher = Pattern.compile("\"feedback\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(reponse);
            return matcher.find()
                    ? matcher.group(1).replace("\\\"", "\"").replace("\\n", " ")
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String genererIndiceBrut(Indice indice) {
        return indice.getContenu();
    }
}
