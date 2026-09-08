package com.xaamxaam.service.exercice;

import com.xaamxaam.config.OcrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Extraction de texte a partir d'une photo d'exercice (OCR), via le service
 * externe OCR.space (https://ocr.space/ocrapi). Choix justifie par le budget
 * de lancement serre (cf. section 7 du cahier des charges) : offre gratuite
 * disponible, pas de dependance native lourde a deployer (contrairement a
 * Tesseract embarque via Tess4J).
 *
 * Pour changer de fournisseur plus tard (ex : Google Cloud Vision), il
 * suffit de remplacer l'implementation de cette classe : le reste de
 * l'application ne depend que de la methode extraireTexte().
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private final OcrProperties properties;
    private final RestClient restClient = RestClient.create();

    private static final long TAILLE_MAX_OCTETS = 5 * 1024 * 1024; // 5 Mo

    public String extraireTexte(MultipartFile image) {
        validerImage(image);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("apikey", properties.apiKey());
        body.add("language", properties.langue() != null ? properties.langue() : "fre");
        body.add("isOverlayRequired", "false");
        body.add("OCREngine", "2"); // moteur 2 : meilleure precision, adapte au texte manuscrit/scolaire
        body.add("file", construireRessourceFichier(image));

        try {
            Map<String, Object> response = restClient.post()
                    .uri(properties.apiUrl())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extraireTexteDeReponse(response);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au service OCR", e);
            throw new IllegalStateException(
                    "L'extraction de texte depuis la photo a echoue. Verifie la qualite de l'image ou reessaie plus tard.", e
            );
        }
    }

    private void validerImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Aucune image fournie.");
        }
        if (image.getSize() > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException("L'image depasse la taille maximale autorisee (5 Mo).");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier fourni n'est pas une image valide.");
        }
    }

    private ByteArrayResource construireRessourceFichier(MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename() != null ? image.getOriginalFilename() : "exercice.jpg";
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier image envoye.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extraireTexteDeReponse(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Reponse vide du service OCR.");
        }

        Boolean erreur = (Boolean) response.get("IsErroredOnProcessing");
        if (Boolean.TRUE.equals(erreur)) {
            Object messageErreur = response.get("ErrorMessage");
            throw new IllegalStateException("Le service OCR a signale une erreur : " + messageErreur);
        }

        List<Map<String, Object>> resultats = (List<Map<String, Object>>) response.get("ParsedResults");
        if (resultats == null || resultats.isEmpty()) {
            throw new IllegalStateException("Aucun texte n'a pu etre extrait de l'image.");
        }

        String texte = (String) resultats.get(0).get("ParsedText");
        if (texte == null || texte.isBlank()) {
            throw new IllegalStateException("Aucun texte lisible n'a ete detecte sur l'image.");
        }

        return texte.trim();
    }
}
