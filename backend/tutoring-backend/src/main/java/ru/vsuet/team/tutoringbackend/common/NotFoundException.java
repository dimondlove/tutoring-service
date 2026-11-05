package ru.vsuet.team.tutoringbackend.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    public NotFoundException(String code, String msg) { super(HttpStatus.NOT_FOUND, code, msg); }
}
