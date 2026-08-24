package site.yesaido.user_server.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.oauth.GoogleLoginRequest;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.Role;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.global.jwt.JwtTokenProvider;
import site.yesaido.user_server.global.oauth.GoogleTokenVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("로그인 기능 테스트")
    class LoginTest {
        @Test
        @DisplayName("성공 : 이메일과 비밀번호가 올바르면 토큰 보따리로 반환")
        void success_login() {
            LoginRequest request = new LoginRequest("test@naver.com", "nhn12345!");
            User user = createUser(1L, request.email(), request.password());

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyLong(), any())).willReturn("mockAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), any())).willReturn("mockRefreshToken");
            given(jwtTokenProvider.getExpirationTime("mockAccessToken")).willReturn(1_755_671_400_000L);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            TokenResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mockAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("mockRefreshToken");
            assertThat(response.getAccessTokenExpiresAt()).isEqualTo(1_755_671_400_000L);
        }

        @Test
        @DisplayName("성공 : Google ID Token이 유효하면 구글 소셜 로그인 성공")
        void loginWithGoogle_success() {
            GoogleLoginRequest request = new GoogleLoginRequest("validIdToken", "google@gmail.com", "구글유저");
            User user = createUser(2L, "google@gmail.com", "");

            given(googleTokenVerifier.verifyAndGetEmail("validIdToken")).willReturn("google@gmail.com");
            given(userRepository.findByEmail("google@gmail.com")).willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(anyLong(), any())).willReturn("mockAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), any())).willReturn("mockRefreshToken");
            given(jwtTokenProvider.getExpirationTime("mockAccessToken")).willReturn(1_755_671_400_000L);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            TokenResponse response = authService.loginWithGoogle(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mockAccessToken");
            assertThat(response.getAccessTokenExpiresAt()).isEqualTo(1_755_671_400_000L);
        }

        @Test
        @DisplayName("실패 : Google ID Token이 유효하지 않으면 InvalidTokenException 발생")
        void loginWithGoogle_invalidIdToken() {
            GoogleLoginRequest request = new GoogleLoginRequest("invalidToken", "google@gmail.com", "구글유저");
            given(googleTokenVerifier.verifyAndGetEmail("invalidToken")).willReturn(null);

            assertThrows(InvalidTokenException.class, () -> authService.loginWithGoogle(request));
        }

        @Test
        @DisplayName("실패 : 존재하지 않는 이메일로 로그인 시 예외가 발생")
        void login_userNotFound() {
            LoginRequest request = new LoginRequest("test@naver.com", "nhn12345!");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("실패 : 비밀번호가 올바르지 않아 예외 발생")
        void login_invalidPassword() {
            LoginRequest request = new LoginRequest("test@naver.com", "nhn12345!");
            User user = createUser(1L, "test@naver.com", "123!encodedPassword");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            assertThrows(InvalidPasswordException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("실패 : 휴면 회원이 로그인 시도 시 DormantUserException 예외 발생")
        void login_dormantUser_failed() {
            LoginRequest request = new LoginRequest("dormant@test.com", "password123@");
            User dormantUser = User.builder()
                    .email("dormant@test.com")
                    .password("encodedPassword")
                    .status(UserStatus.DORMANT)
                    .build();
            given(userRepository.findByEmail("dormant@test.com")).willReturn(Optional.of(dormantUser));
            given(passwordEncoder.matches(request.password(), dormantUser.getPassword())).willReturn(true);

            assertThrows(DormantUserException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("실패 : 탈퇴한 사용자로 로그인 시 예외 발생")
        void login_alreadyWithdrawn() {
            LoginRequest request = new LoginRequest("test@naver.com", "nhn12345!");
            User withdrawnUser = User.builder()
                    .id(1L)
                    .email("test@naver.com")
                    .password("nhn12345!")
                    .status(UserStatus.DELETED)
                    .build();

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(withdrawnUser));
            given(passwordEncoder.matches(request.password(), withdrawnUser.getPassword())).willReturn(true);

            assertThrows(AlreadyWithdrawnException.class, () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("토큰 재발급(reissue) 기능 테스트")
    class ReissueTest {
        @Test
        @DisplayName("성공 : 유효한 RefreshToken이면 새로운 토큰 반환")
        void reissue_success() {
            String refreshToken = "validRefreshToken";
            Long userId = 1L;
            User user = createUser(userId, "test@naver.com", "password");

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("RT:" + userId)).willReturn(refreshToken);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(anyLong(), any())).willReturn("newAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), any())).willReturn("newRefreshToken");
            given(jwtTokenProvider.getExpirationTime("newAccessToken")).willReturn(1_755_671_400_000L);

            TokenResponse response = authService.reissue(refreshToken);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
            assertThat(response.getAccessTokenExpiresAt()).isEqualTo(1_755_671_400_000L);
        }

        @Test
        @DisplayName("실패 : 만료되거나 유효하지 않은 RefreshToken이면 예외 발생")
        void reissue_invalidJwtToken() {
            String invalidToken = "invalidToken";
            given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

            assertThrows(InvalidTokenException.class, () -> authService.reissue(invalidToken));
        }

        @Test
        @DisplayName("실패 : 레디스에 저장된 토큰과 다르면 예외 발생")
        void reissue_redisMismatch() {
            String refreshToken = "validToken";
            Long userId = 1L;

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("RT:" + userId)).willReturn("differentTokenInRedis");

            assertThrows(InvalidTokenException.class, () -> authService.reissue(refreshToken));
        }
    }

    @Nested
    @DisplayName("로그아웃 기능 테스트")
    class LogoutTest {
        @Test
        @DisplayName("성공 : 로그아웃 시 레디스의 RefreshToken 키 삭제")
        void logout_success() {
            Long userId = 1L;
            authService.logout(userId);

            verify(stringRedisTemplate).delete("RT:" + userId);
        }
    }

    public User createUser(Long id, String email, String password) {
        return User.builder()
                .id(id)
                .email(email)
                .password(password)
                .role(Role.USER)
                .nickName("newNick")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("휴면 계정 테스트")
    class DormantTest {
        @Test
        @DisplayName("성공 : 휴면 회원 이메일로 해제 시 상태가 ACTIVE로 전환된다")
        void releaseDormant_success() {
            User dormantUser = User.builder()
                    .email("dormant@test.com")
                    .status(UserStatus.DORMANT)
                    .build();

            given(userRepository.findByEmail("dormant@test.com")).willReturn(Optional.of(dormantUser));

            authService.releaseDormant("dormant@test.com");

            assertThat(dormantUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    @Test
    @DisplayName("실패 : 존재하지 않는 이메일로 휴면 해제 시도 시 UserNotFoundException 발생")
    void releaseDormant_UserNotFound_failure() {
        given(userRepository.findByEmail("noexist@test.com")).willReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.releaseDormant("noexist@test.com"));
    }
}
