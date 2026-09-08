package com.xaamxaam.client;

import com.xaamxaam.config.ClaudeApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Client HTTP vers l'API Anthropic Claude (/v1/messages).
 * Isole intentionnellement dans son propre package pour pouvoir etre
 * mocke dans les tests et pour changer de modele facilement
 * (ex : Haiku pour les indices, un modele plus capable pour la
 * reformulation finale) sans toucher au reste du code.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeApiClient {

    private final ClaudeApiProperties properties;
    private final RestClient restClient = RestClient.create();

    public String envoyerMessage(String systemPrompt, String messageUtilisateur) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            // Echec rapide et explicite plutot que d'envoyer une requete
            // qui echouera de toute facon en 401 avec un message opaque.
            log.error("ANTHROPIC_API_KEY n'est pas configuree - impossible d'appeler l'API Claude.");
            throw new IllegalStateException(
                    "Le service IA n'est pas configure (cle API manquante). Contacte l'administrateur."
            );
        }

        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", messageUtilisateur))
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(properties.apiUrl())
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", properties.anthropicVersion())
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extraireTexte(response);
        } catch (RestClientResponseException e) {
            // Cas le plus utile a diagnostiquer : Anthropic renvoie un
            // code d'erreur HTTP (401 cle invalide, 404 modele inconnu,
            // 429 rate limit...) accompagne d'un corps JSON explicite.
            // On le logue en entier cote serveur (jamais renvoye au
            // client, qui ne voit qu'un message generique).
            log.error(
                    "Erreur de l'API Claude - statut {} - corps de la reponse : {}",
                    e.getStatusCode(), e.getResponseBodyAsString()
            );
            throw new IllegalStateException("Le service IA est momentanement indisponible. Reessaie plus tard.", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'appel a l'API Claude", e);
            throw new IllegalStateException("Le service IA est momentanement indisponible. Reessaie plus tard.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extraireTexte(Map<String, Object> response) {
        if (response == null || !response.containsKey("content")) {
            return "";
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> block : content) {
            if ("text".equals(block.get("type"))) {
                sb.append(block.get("text"));
            }
        }
        return sb.toString();
    }
}
