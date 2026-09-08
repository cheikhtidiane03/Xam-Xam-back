package com.xaamxaam.exception;

/** Leve quand trop de tentatives de connexion echouees ont ete faites recemment pour un email. */
public class CompteTemporairementBloqueException extends RuntimeException {
    public CompteTemporairementBloqueException(String message) {
        super(message);
    }
}
