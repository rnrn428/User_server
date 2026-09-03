package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class DormantUserException extends BadRequestException {
    private static final String MESSAGE = "휴면 처리된 계정입니다.";

    public DormantUserException(){
        super(MESSAGE);
    }

    public DormantUserException(String message) {
        super(message);
    }
}
