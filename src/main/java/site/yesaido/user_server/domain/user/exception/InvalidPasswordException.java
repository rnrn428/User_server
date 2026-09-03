package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidPasswordException extends BadRequestException {

    private static final String MESSAGE = "비밀번호가 일치하지 않습니다.";

    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException() {
        super(MESSAGE);
    }
}
