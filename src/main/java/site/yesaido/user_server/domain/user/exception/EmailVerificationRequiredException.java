package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class EmailVerificationRequiredException extends BadRequestException {
    public EmailVerificationRequiredException() {
        super("이메일 인증을 완료해 주세요.");
    }
}
