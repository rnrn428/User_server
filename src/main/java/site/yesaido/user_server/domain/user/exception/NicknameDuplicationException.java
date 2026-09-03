package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.ConflictException;

public class NicknameDuplicationException extends ConflictException {
    private static final String DEFAULT_MESSAGE = "이미 사용 중인 닉네임입니다.";

    public NicknameDuplicationException() {
        super(DEFAULT_MESSAGE);
    }

    public NicknameDuplicationException(String message) {
        super(message);
    }
}
