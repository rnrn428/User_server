package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "존재하지 않는 사용자입니다.";

    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
