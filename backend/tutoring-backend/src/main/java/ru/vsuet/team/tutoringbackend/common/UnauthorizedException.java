package ru.vsuet.team.tutoringbackend.common;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String code, String msg) { super(HttpStatus.UNAUTHORIZED, code, msg); }
}
