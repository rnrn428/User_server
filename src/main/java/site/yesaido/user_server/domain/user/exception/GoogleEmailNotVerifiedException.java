package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class GoogleEmailNotVerifiedException extends BadRequestException {
    private static final String DEFAULT_MESSAGE = "Google 이메일 인증이 완료되지 않은 계정입니다.";

    public GoogleEmailNotVerifiedException() {
        super(DEFAULT_MESSAGE);
    }

    public GoogleEmailNotVerifiedException(String message) {
        super(message);
    }
}