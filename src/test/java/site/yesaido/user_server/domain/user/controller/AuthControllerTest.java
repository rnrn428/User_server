package site.yesaido.user_server.domain.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.login.PasswordResetRequest;
import site.yesaido.user_server.domain.user.dto.token.LogoutRequest;
import site.yesaido.user_server.domain.user.dto.token.ReissueRequest;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.service.AuthService;
import site.yesaido.user_server.global.common.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("로그인 성공 시 200과 토큰 응답을 반환한다")
    void login_success() {
        LoginRequest request = new LoginRequest("test@test.com", "password123!");
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        given(authService.login(request)).willReturn(tokenResponse);

        ResponseEntity<ApiResponse<TokenResponse>> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getAccessToken()).isEqualTo("access-token");
        assertThat(response.getBody().data().getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 200과 새로운 토큰 응답을 반환한다")
    void reissue_success(){
        ReissueRequest request = new ReissueRequest("oldRefreshToken");
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .build();

        given(authService.reissue(request.getRefreshToken())).willReturn(tokenResponse);

        ResponseEntity<ApiResponse<TokenResponse>> response = authController.reissue(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getBody().data().getRefreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("Refresh Token 로그아웃에 성공하면 204를 반환한다")
    void logout_success(){

        LogoutRequest request = new LogoutRequest("refresh-token", "access-token");

        ResponseEntity<Void> response = authController.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout("refresh-token", "access-token");
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 시 200 OK와 성공 메시지를 반환한다")
    void resetPassword_success() {
        PasswordResetRequest request = new PasswordResetRequest("test@test.com", "newPassword123!");

        ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("비밀번호가 변경되었습니다.");
        verify(authService).resetPassword(request);
    }
}






