package site.yesaido.user_server.domain.email.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.user_server.domain.email.dto.EmailSendRequest;
import site.yesaido.user_server.domain.email.dto.EmailVerifyRequest;
import site.yesaido.user_server.global.common.ApiResponse;

/**
 * {@code EmailController}의 OpenAPI 문서 정의.
 */
@Tag(name = "이메일 인증", description = "이메일 인증번호 발송 및 검증")
public interface EmailControllerDocs {

    @Operation(summary = "인증번호 발송", description = "입력한 이메일로 인증번호를 발송합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<Void>> sendEmail(EmailSendRequest request);

    @Operation(summary = "인증번호 검증", description = "이메일과 인증번호가 일치하는지 검증합니다. `data`가 검증 결과입니다.")
    ResponseEntity<ApiResponse<Boolean>> verifyEmail(EmailVerifyRequest request);
}
