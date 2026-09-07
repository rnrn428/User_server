package site.yesaido.user_server.domain.user.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import site.yesaido.user_server.domain.email.dto.EmailSendRequest;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.login.PasswordResetRequest;
import site.yesaido.user_server.domain.user.dto.oauth.GoogleLoginRequest;
import site.yesaido.user_server.domain.user.dto.token.LogoutRequest;
import site.yesaido.user_server.domain.user.dto.token.ReissueRequest;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.global.common.ApiResponse;

/**
 * {@code AuthController}의 OpenAPI 문서 정의.
 */
@Tag(name = "인증", description = "로그인 · 토큰 재발급 · 로그아웃 · 소셜 로그인 · 휴면 해제 · 비밀번호 재설정")
public interface AuthControllerDocs {

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 access/refresh 토큰을 발급합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<TokenResponse>> login(LoginRequest request);

    @Operation(summary = "토큰 재발급", description = "refresh 토큰으로 access/refresh 토큰을 재발급합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<TokenResponse>> reissue(ReissueRequest reissueRequest);

    @Operation(summary = "로그아웃",
            description = "refresh 토큰을 폐기하고 access 토큰을 블랙리스트에 등록합니다.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃됨"))
    ResponseEntity<Void> logout(LogoutRequest request);

    @Operation(summary = "휴면 계정 해제", description = "이메일로 식별되는 휴면 계정을 활성 상태로 되돌립니다. (인증 불필요)")
    ResponseEntity<ApiResponse<Void>> releaseDormant(@Parameter(description = "대상 이메일") String email);

    @Operation(summary = "Google 소셜 로그인", description = "Google id_token 으로 로그인/회원가입하고 토큰을 발급합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<TokenResponse>> loginWithGoogle(GoogleLoginRequest request);

    @Operation(summary = "비밀번호 재설정", description = "이메일 인증을 통과한 사용자의 비밀번호를 재설정합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<Void>> resetPassword(PasswordResetRequest resetRequest);

    @Operation(summary = "비밀번호 재설정 인증번호 발송", description = "비밀번호 재설정용 인증번호를 이메일로 발송합니다. (인증 불필요)")
    ResponseEntity<ApiResponse<Void>> sendPasswordResetEmail(EmailSendRequest request);
}
