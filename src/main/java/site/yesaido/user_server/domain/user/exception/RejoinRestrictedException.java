package site.yesaido.user_server.domain.user.exception;

import site.yesaido.common.exception.client.BadRequestException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RejoinRestrictedException extends BadRequestException {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public RejoinRestrictedException(LocalDateTime rejoinAvailableAt) {
        super("탈퇴 처리 후 30일 동안은 재가입할 수 없습니다. "
                + rejoinAvailableAt.format(DATE_FORMATTER) + "부터 재가입할 수 있습니다.");
    }
}
