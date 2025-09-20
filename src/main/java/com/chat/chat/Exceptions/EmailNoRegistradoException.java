package com.chat.chat.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailNoRegistradoException extends RuntimeException {
    public EmailNoRegistradoException() { super("Email incorrecto"); }
}
