package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidMemberStatusException extends BadRequestException {
    public InvalidMemberStatusException(String message) {
        super(message);
    }
}
