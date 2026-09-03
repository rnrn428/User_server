package site.yesaido.user_server.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.yesaido.user_server.domain.email.service.EmailService;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.login.PasswordResetRequest;
import site.yesaido.user_server.domain.user.dto.oauth.GoogleLoginRequest;
import site.yesaido.user_server.domain.user.dto.token.RefreshTokenRotation;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.entity.OAuthUser;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.DormantUserException;
import site.yesaido.user_server.domain.user.exception.InvalidPasswordException;
import site.yesaido.user_server.domain.user.exception.InvalidTokenException;
import site.yesaido.user_server.domain.user.exception.SocialLoginPasswordResetNotAllowedException;
import site.yesaido.user_server.domain.user.repository.OAuthUserRepository;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.jwt.AccessTokenBlacklistService;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenGraceService;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenService;
import site.yesaido.user_server.global.jwt.AccessTokenProvider;
import site.yesaido.user_server.global.oauth.GoogleIdentity;
import site.yesaido.user_server.global.oauth.GoogleTokenVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccessTokenProvider accessTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RefreshTokenGraceService refreshTokenGraceService;
    @Mock private AccessTokenBlacklistService accessTokenBlacklistService;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private EmailService emailService;
    @Mock private OAuthUserRepository oAuthUserRepository;

    @InjectMocks private AuthService authService;

    @Nested
    @DisplayName("로그인")
    class LoginTest {
        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 Access Token과 새 Refresh Token을 반환한다")
        void loginSuccess() {
            LoginRequest request = new LoginRequest("test@naver.com", "password");
            User user = createUser(1L, request.email(), "encoded-password");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(refreshTokenService.createRefreshTokenForUser(user.getId())).willReturn("refresh-token");
            given(accessTokenProvider.createAccessToken(user.getId(), user.getRole())).willReturn("access-token");
            given(accessTokenProvider.getExpirationTime("access-token")).willReturn(1_755_671_400_000L);

            TokenResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            verify(refreshTokenService).createRefreshTokenForUser(user.getId());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 토큰을 발급하지 않는다")
        void loginWithWrongPasswordThrowsException() {
            LoginRequest request = new LoginRequest("test@naver.com", "wrong-password");
            User user = createUser(1L, request.email(), "encoded-password");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            assertThrows(InvalidPasswordException.class, () -> authService.login(request));
            verify(refreshTokenService, never()).createRefreshTokenForUser(anyLong());
        }

        @Test
        @DisplayName("휴면 사용자는 Refresh Token을 발급하지 않는다")
        void dormantUserDoesNotReceiveRefreshToken() {
            LoginRequest request = new LoginRequest("test@naver.com", "password");
            User user = createUser(1L, request.email(), "encoded-password");
            user.changeToDormant();
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);

            assertThrows(DormantUserException.class, () -> authService.login(request));
            verify(refreshTokenService, never()).createRefreshTokenForUser(anyLong());
        }

        @Test
        @DisplayName("이미 연결된 Google 계정은 sub로 기존 회원을 찾아 로그인한다")
        void googleLoginWithExistingOAuthLink() {
            GoogleLoginRequest request = new GoogleLoginRequest("google-id-token", "ignored@email.com", "nickname");
            User user = createUser(2L, "google@gmail.com", null);
            GoogleIdentity identity = new GoogleIdentity("google-subject", "google@gmail.com", "Google User");
            OAuthUser oauthUser = OAuthUser.builder().user(user).provider("GOOGLE")
                    .providerSubjectId(identity.subject()).build();
            given(googleTokenVerifier.verify(request.idToken())).willReturn(identity);
            given(oAuthUserRepository.findByProviderAndProviderSubjectId("GOOGLE", identity.subject()))
                    .willReturn(Optional.of(oauthUser));
            given(refreshTokenService.createRefreshTokenForUser(user.getId())).willReturn("refresh-token");
            given(accessTokenProvider.createAccessToken(user.getId(), user.getRole())).willReturn("access-token");
            given(accessTokenProvider.getExpirationTime("access-token")).willReturn(1L);

            assertThat(authService.loginWithGoogle(request).getAccessToken()).isEqualTo("access-token");

            verify(userRepository, never()).findByEmail(anyString());
            verify(oAuthUserRepository, never()).save(org.mockito.ArgumentMatchers.any(OAuthUser.class));
        }

        @Test
        @DisplayName("처음 온 Google 계정과 동일 이메일의 로컬 회원을 연결한다")
        void googleLoginLinksExistingLocalUser() {
            GoogleLoginRequest request = new GoogleLoginRequest("google-id-token", "ignored@email.com", "nickname");
            GoogleIdentity identity = new GoogleIdentity("google-subject", "local@gmail.com", "Google User");
            User localUser = createUser(3L, identity.email(), "encoded-password");
            given(googleTokenVerifier.verify(request.idToken())).willReturn(identity);
            given(oAuthUserRepository.findByProviderAndProviderSubjectId("GOOGLE", identity.subject()))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(identity.email())).willReturn(Optional.of(localUser));
            given(refreshTokenService.createRefreshTokenForUser(localUser.getId())).willReturn("refresh-token");
            given(accessTokenProvider.createAccessToken(localUser.getId(), localUser.getRole())).willReturn("access-token");
            given(accessTokenProvider.getExpirationTime("access-token")).willReturn(1L);

            authService.loginWithGoogle(request);

            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
            verify(oAuthUserRepository).save(org.mockito.ArgumentMatchers.argThat(oauthUser ->
                    oauthUser.getUser() == localUser
                            && "GOOGLE".equals(oauthUser.getProvider())
                            && identity.subject().equals(oauthUser.getProviderSubjectId())
            ));
        }

        @Test
        @DisplayName("처음 온 Google 계정은 새 회원과 OAuth 연결 기록을 생성한다")
        void googleLoginCreatesUserAndOAuthLink() {
            GoogleLoginRequest request = new GoogleLoginRequest("google-id-token", "ignored@email.com", "nickname");
            GoogleIdentity identity = new GoogleIdentity("google-subject", "new@gmail.com", "New Google User");
            User newUser = createUser(4L, identity.email(), null);
            given(googleTokenVerifier.verify(request.idToken())).willReturn(identity);
            given(oAuthUserRepository.findByProviderAndProviderSubjectId("GOOGLE", identity.subject()))
                    .willReturn(Optional.empty());
            given(userRepository.findByEmail(identity.email())).willReturn(Optional.empty());
            given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).willReturn(newUser);
            given(refreshTokenService.createRefreshTokenForUser(newUser.getId())).willReturn("refresh-token");
            given(accessTokenProvider.createAccessToken(newUser.getId(), newUser.getRole())).willReturn("access-token");
            given(accessTokenProvider.getExpirationTime("access-token")).willReturn(1L);

            authService.loginWithGoogle(request);

            verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
            verify(oAuthUserRepository).save(org.mockito.ArgumentMatchers.argThat(oauthUser ->
                    oauthUser.getUser() == newUser
                            && "GOOGLE".equals(oauthUser.getProvider())
                            && identity.subject().equals(oauthUser.getProviderSubjectId())
            ));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueTest {
        @Test
        @DisplayName("유효한 Refresh Token은 교체되고 새 Access Token과 함께 반환된다")
        void reissueSuccess() {
            User user = createUser(1L, "test@naver.com", "encoded-password");
            given(refreshTokenService.rotateRefreshToken("old-refresh"))
                    .willReturn(new RefreshTokenRotation(1L, "new-refresh"));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(accessTokenProvider.createAccessToken(1L, Role.USER)).willReturn("new-access");
            given(accessTokenProvider.getExpirationTime("new-access")).willReturn(2L);

            TokenResponse response = authService.reissue("old-refresh");

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            verify(refreshTokenGraceService).saveReissueResponse("old-refresh", response);
        }

        @Test
        @DisplayName("동시에 들어온 재발급은 살아 있는 Grace 응답을 반환한다")
        void reissueReturnsGraceResponse() {
            TokenResponse graceResponse = TokenResponse.builder()
                    .accessToken("grace-access").refreshToken("grace-refresh")
                    .role(Role.USER).accessTokenExpiresAt(3L).build();
            given(refreshTokenService.rotateRefreshToken("old-refresh"))
                    .willThrow(new InvalidTokenException("already rotated"));
            given(refreshTokenGraceService.findReissueResponse("old-refresh"))
                    .willReturn(Optional.of(graceResponse));
            given(refreshTokenService.isRefreshTokenActive("grace-refresh")).willReturn(true);

            assertThat(authService.reissue("old-refresh")).isSameAs(graceResponse);
        }

        @Test
        @DisplayName("Grace 응답이 없으면 재발급 실패 예외를 반환한다")
        void reissueWithoutGraceResponseThrowsException() {
            InvalidTokenException exception = new InvalidTokenException("invalid refresh token");
            given(refreshTokenService.rotateRefreshToken("invalid-refresh")).willThrow(exception);
            given(refreshTokenGraceService.findReissueResponse("invalid-refresh"))
                    .willReturn(Optional.empty());

            assertThat(assertThrows(InvalidTokenException.class,
                    () -> authService.reissue("invalid-refresh"))).isSameAs(exception);
        }
    }

    @Test
    @DisplayName("로그아웃하면 현재 RT를 폐기하고 유효한 AT를 블랙리스트에 등록한다")
    void logoutRevokesCurrentRefreshTokenAndBlacklistsAccessToken() {
        authService.logout("refresh-token", "access-token");
        verify(refreshTokenService).revokeRefreshToken("refresh-token");
        verify(accessTokenBlacklistService).blacklist("access-token");
    }

    @Test
    @DisplayName("Access Token이 없어도 Refresh Token 로그아웃은 처리한다")
    void logoutWithoutAccessTokenStillRevokesRefreshToken() {
        authService.logout("refresh-token", null);

        verify(refreshTokenService).revokeRefreshToken("refresh-token");
        verify(accessTokenBlacklistService, never()).blacklist(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("비밀번호를 변경하면 모든 기기의 Refresh Token을 폐기한다")
    void resetPasswordRevokesAllRefreshTokens() {
        PasswordResetRequest request = new PasswordResetRequest("test@naver.com", "new-password");
        User user = createUser(1L, request.email(), "old-password");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.encode(request.newPassword())).willReturn("encoded-new-password");

        authService.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        verify(refreshTokenService).revokeAllRefreshTokens(user.getId());
    }

    @Test
    @DisplayName("기존 비밀번호와 같은 값으로는 비밀번호를 재설정할 수 없다")
    void resetPasswordWithSamePasswordThrowsException() {
        PasswordResetRequest request = new PasswordResetRequest("test@naver.com", "same-password");
        User user = createUser(1L, request.email(), "encoded-current-password");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.newPassword(), user.getPassword())).willReturn(true);

        InvalidPasswordException exception = assertThrows(
                InvalidPasswordException.class,
                () -> authService.resetPassword(request)
        );

        assertThat(exception).hasMessage("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        assertThat(user.getPassword()).isEqualTo("encoded-current-password");
        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenService, never()).revokeAllRefreshTokens(user.getId());
    }

    @Test
    @DisplayName("소셜 로그인 전용 계정은 비밀번호를 재설정할 수 없다")
    void resetPasswordForSocialOnlyAccountThrowsException() {
        PasswordResetRequest request = new PasswordResetRequest("social@naver.com", "new-password");
        User socialUser = createUser(1L, request.email(), null);
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(socialUser));

        assertThrows(
                SocialLoginPasswordResetNotAllowedException.class,
                () -> authService.resetPassword(request)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenService, never()).revokeAllRefreshTokens(socialUser.getId());
    }

    @Test
    @DisplayName("소셜 로그인 전용 계정에는 비밀번호 찾기 인증코드를 발송하지 않는다")
    void sendPasswordResetEmailForSocialOnlyAccountThrowsException() {
        String email = "social@naver.com";
        User socialUser = createUser(1L, email, null);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(socialUser));

        assertThrows(
                SocialLoginPasswordResetNotAllowedException.class,
                () -> authService.sendPasswordResetEmail(email)
        );

        verify(emailService, never()).sendVerificationEmail(email);
    }

    @Test
    @DisplayName("휴면 계정 해제 시 사용 가능 상태로 전환한다")
    void releaseDormantActivatesUser() {
        User user = createUser(1L, "dormant@naver.com", "password");
        user.changeToDormant();
        given(userRepository.findByEmail("dormant@naver.com")).willReturn(Optional.of(user));

        authService.releaseDormant("dormant@naver.com");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private User createUser(Long id, String email, String password) {
        return User.builder().id(id).email(email).password(password).role(Role.USER)
                .nickName("nickname").status(UserStatus.ACTIVE).build();
    }
}
