package com.xaamxaam.exception;

/** Leve quand un eleve depasse son quota quotidien de requetes IA (gestion du cout). */
public class QuotaDepasseException extends RuntimeException {
    public QuotaDepasseException(String message) {
        super(message);
    }
}
