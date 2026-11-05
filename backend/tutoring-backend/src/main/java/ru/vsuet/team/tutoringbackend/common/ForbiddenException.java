package ru.vsuet.team.tutoringbackend.common;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String code, String msg) { super(HttpStatus.FORBIDDEN, code, msg); }
}
