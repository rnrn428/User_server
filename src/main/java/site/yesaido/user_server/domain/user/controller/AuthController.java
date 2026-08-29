package site.yesaido.user_server.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.login.PasswordResetRequest;
import site.yesaido.user_server.domain.user.dto.oauth.GoogleLoginRequest;
import site.yesaido.user_server.domain.user.dto.token.LogoutRequest;
import site.yesaido.user_server.domain.user.dto.token.ReissueRequest;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.service.AuthService;
import site.yesaido.user_server.global.common.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request){
        TokenResponse response = authService.login(request);
        ApiResponse<TokenResponse> apiResponse = ApiResponse.ok("로그인에 성공하였습니다", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest reissueRequest){
        TokenResponse response = authService.reissue(reissueRequest.getRefreshToken());
        ApiResponse<TokenResponse> apiResponse = ApiResponse.ok("토큰이 성공적으로 재발급되었습니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request){
        authService.logout(request.refreshToken(), request.accessToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/dormant/release")
    public ResponseEntity<ApiResponse<Void>> releaseDormant(@RequestParam("email") String email){
        authService.releaseDormant(email);
        ApiResponse<Void> apiResponse = ApiResponse.ok("휴면 계정이 성공적으로 해제되었습니다.");
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping("/oauth2/google")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request){
        TokenResponse response = authService.loginWithGoogle(request);
        ApiResponse<TokenResponse> apiResponse = ApiResponse.ok("구글 소셜 로그인에 성공하였습니다.", response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest){
        authService.resetPassword(resetRequest);

        ApiResponse<Void> response = ApiResponse.ok("비밀번호가 변경되었습니다.");
        return ResponseEntity.status(response.httpStatus()).body(response);
    }


}
