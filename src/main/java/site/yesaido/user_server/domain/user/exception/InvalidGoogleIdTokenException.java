package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.UnauthorizedException;

public class InvalidGoogleIdTokenException extends UnauthorizedException {
    private static final String DEFAULT_MESSAGE = "유효하지 않은 Google ID Token입니다.";

    public InvalidGoogleIdTokenException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidGoogleIdTokenException(String message) {
        super(message);
    }
}