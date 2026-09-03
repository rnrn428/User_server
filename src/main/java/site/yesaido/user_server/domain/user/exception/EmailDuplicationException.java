package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.ConflictException;

public class EmailDuplicationException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "이미 사용 중인 이메일입니다.";

    public EmailDuplicationException() {
        super(DEFAULT_MESSAGE);
    }

    public EmailDuplicationException(String message) {
        super(message);
    }
}
