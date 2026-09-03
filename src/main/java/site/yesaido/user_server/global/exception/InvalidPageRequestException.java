package site.yesaido.user_server.global.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidPageRequestException extends BadRequestException {
    public InvalidPageRequestException(String message) {
        super(message);
    }
}