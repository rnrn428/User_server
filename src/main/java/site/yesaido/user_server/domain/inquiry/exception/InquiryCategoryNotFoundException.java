package site.yesaido.user_server.domain.inquiry.exception;

import site.yesaido.common.exception.client.NotFoundException;

public class InquiryCategoryNotFoundException extends NotFoundException {
    private static final String DEFAULT_MESSAGE = "존재하지 않는 문의 카테고리입니다.";
    public InquiryCategoryNotFoundException() { super(DEFAULT_MESSAGE); }
    public InquiryCategoryNotFoundException(String message) {
        super(message);
    }
}
