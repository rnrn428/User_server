package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InvalidForceWithdrawalException extends BadRequestException {
    public InvalidForceWithdrawalException(String message) {
        super(message);
    }
}
