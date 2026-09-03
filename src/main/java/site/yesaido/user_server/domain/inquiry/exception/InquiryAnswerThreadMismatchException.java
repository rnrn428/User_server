package site.yesaido.user_server.domain.inquiry.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InquiryAnswerThreadMismatchException extends BadRequestException {
    private static final String DEFAULT_MESSAGE = "이전 메시지가 다른 문의에 속해 있어 현재 문의의 스레드로 연결할 수 없습니다.";

    public InquiryAnswerThreadMismatchException() { super(DEFAULT_MESSAGE); }
    public InquiryAnswerThreadMismatchException(String message) {
        super(message);
    }
}
