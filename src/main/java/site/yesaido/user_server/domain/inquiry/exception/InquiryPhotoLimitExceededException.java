package site.yesaido.user_server.domain.inquiry.exception;

import site.yesaido.common.exception.client.BadRequestException;

public class InquiryPhotoLimitExceededException extends BadRequestException {
    public InquiryPhotoLimitExceededException(String message) {
        super(message);
    }
}
