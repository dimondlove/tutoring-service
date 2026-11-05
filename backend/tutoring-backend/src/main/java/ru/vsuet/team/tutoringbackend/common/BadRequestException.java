package ru.vsuet.team.tutoringbackend.common;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String code, String msg) { super(HttpStatus.BAD_REQUEST, code, msg); }
}
