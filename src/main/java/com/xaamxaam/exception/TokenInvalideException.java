package com.xaamxaam.exception;

/** Leve quand un token (reinitialisation mot de passe, verification email) est invalide, deja utilise ou expire. */
public class TokenInvalideException extends RuntimeException {
    public TokenInvalideException(String message) {
        super(message);
    }
}
