package ru.vsuet.team.tutoringbackend.common;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException(String code, String msg) { super(HttpStatus.CONFLICT, code, msg); }
}
