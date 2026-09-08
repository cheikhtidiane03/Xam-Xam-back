package com.xaamxaam.exception;

/** Leve quand un utilisateur tente d'acceder a une ressource qui ne lui appartient pas
 *  (ex : un eleve consultant l'exercice d'un autre eleve). */
public class AccesRefuseException extends RuntimeException {
    public AccesRefuseException(String message) {
        super(message);
    }
}
