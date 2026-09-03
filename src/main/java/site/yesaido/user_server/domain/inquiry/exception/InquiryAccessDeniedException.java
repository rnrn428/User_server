package site.yesaido.user_server.domain.inquiry.exception;

import site.yesaido.common.exception.client.ForbiddenException;

public class InquiryAccessDeniedException extends ForbiddenException {
    private static final String DEFAULT_MESSAGE = "해당 문의에 접근할 권한이 없습니다.";

    public InquiryAccessDeniedException() { super(DEFAULT_MESSAGE); }
    public InquiryAccessDeniedException(String message) {
        super(message);
    }
}
