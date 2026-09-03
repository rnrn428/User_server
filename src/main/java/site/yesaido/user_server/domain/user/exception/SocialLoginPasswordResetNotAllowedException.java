package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class SocialLoginPasswordResetNotAllowedException extends BadRequestException {
    public SocialLoginPasswordResetNotAllowedException(String message) {
        super(message);
    }
}
