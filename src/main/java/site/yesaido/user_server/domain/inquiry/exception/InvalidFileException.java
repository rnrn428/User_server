package site.yesaido.user_server.domain.inquiry.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidFileException extends BadRequestException {
    public InvalidFileException(String message) {
        super(message);
    }
}
