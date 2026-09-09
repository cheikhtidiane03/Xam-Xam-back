package com.xaamxaam.service.notification;

import com.xaamxaam.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Point d'entree unique pour tous les emails transactionnels envoyes par
 * la plateforme (verification de compte, reinitialisation de mot de passe,
 * rapport hebdomadaire parent). Centralise ici pour ne changer qu'un seul
 * endroit si le fournisseur SMTP change plus tard (ex : passage a un
 * service transactionnel type Brevo/Sendinblue plutot que du SMTP direct).
 *
 * IMPORTANT : @Async. L'envoi d'email ne doit JAMAIS bloquer la reponse
 * HTTP d'une action metier (inscription, mot de passe oublie...). Sans ca,
 * un SMTP mal configure ou injoignable (host/identifiants vides, comme en
 * environnement de demo sans SMTP configure) fait attendre la requete
 * entrante pendant tout le timeout de connexion reseau par defaut de
 * Java, potentiellement plusieurs minutes - c'est exactement ce qui
 * rendait /api/auth/register anormalement lent en production.
 * Necessite @EnableAsync sur la classe principale (voir XaamXaamApplication).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Async
    public void envoyerEmailHtml(String destinataire, String sujet, String corpsHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailProperties.expediteur());
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corpsHtml, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            // On ne fait jamais echouer l'action metier (inscription, etc.) a cause
            // d'un probleme d'envoi d'email : on logue et on continue.
            log.error("Echec de l'envoi d'email a {} (sujet: {})", destinataire, sujet, e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi d'email a {}", destinataire, e);
        }
    }

    public void envoyerVerificationEmail(String destinataire, String prenom, String token) {
        String lien = mailProperties.urlFrontend() + "/verifier-email?token=" + token;
        String corps = """
                <p>Bonjour %s,</p>
                <p>Merci de confirmer ton adresse email pour activer toutes les fonctionnalites de ton compte Xaam-Xaam+ :</p>
                <p><a href="%s">Confirmer mon email</a></p>
                <p>Ce lien expire dans 24 heures.</p>
                """.formatted(prenom, lien);
        envoyerEmailHtml(destinataire, "Confirme ton email Xaam-Xaam+", corps);
    }

    public void envoyerReinitialisationMotDePasse(String destinataire, String prenom, String token) {
        String lien = mailProperties.urlFrontend() + "/reinitialiser-mot-de-passe?token=" + token;
        String corps = """
                <p>Bonjour %s,</p>
                <p>Tu as demande la reinitialisation de ton mot de passe Xaam-Xaam+.</p>
                <p><a href="%s">Choisir un nouveau mot de passe</a></p>
                <p>Ce lien expire dans 1 heure. Si tu n'es pas a l'origine de cette demande, ignore cet email.</p>
                """.formatted(prenom, lien);
        envoyerEmailHtml(destinataire, "Reinitialisation de ton mot de passe Xaam-Xaam+", corps);
    }

    public void envoyerRapportHebdomadaire(String destinataire, String prenomParent, String corpsHtml) {
        envoyerEmailHtml(destinataire, "Ton rapport hebdomadaire Xaam-Xaam+", corpsHtml);
    }
}
