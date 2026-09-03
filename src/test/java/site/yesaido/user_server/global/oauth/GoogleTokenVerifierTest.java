package site.yesaido.user_server.global.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import site.yesaido.user_server.domain.user.exception.GoogleEmailNotVerifiedException;
import site.yesaido.user_server.domain.user.exception.InvalidGoogleIdTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierTest {

    @Mock private JwtDecoder googleJwtDecoder;
    @Mock private Jwt jwt;
    @InjectMocks private GoogleTokenVerifier googleTokenVerifier;

    @Test
    @DisplayName("검증된 Google ID 토큰에서 sub, 이메일, 이름을 반환한다")
    void verifyReturnsGoogleIdentity() {
        given(googleJwtDecoder.decode("valid-token")).willReturn(jwt);
        given(jwt.getClaim("email_verified")).willReturn(true);
        given(jwt.getSubject()).willReturn("google-subject");
        given(jwt.getClaimAsString("email")).willReturn("user@gmail.com");
        given(jwt.getClaimAsString("name")).willReturn("Google User");

        GoogleIdentity identity = googleTokenVerifier.verify("valid-token");

        assertThat(identity).isEqualTo(new GoogleIdentity(
                "google-subject", "user@gmail.com", "Google User"
        ));
    }

    @Test
    @DisplayName("이메일 인증이 완료되지 않은 Google 계정은 거절한다")
    void verifyRejectsUnverifiedEmail() {
        given(googleJwtDecoder.decode("unverified-token")).willReturn(jwt);
        given(jwt.getClaim("email_verified")).willReturn(false);

        assertThrows(GoogleEmailNotVerifiedException.class,
                () -> googleTokenVerifier.verify("unverified-token"));
    }

    @Test
    @DisplayName("JWT 검증에 실패하면 유효하지 않은 Google 토큰으로 처리한다")
    void verifyRejectsInvalidToken() {
        given(googleJwtDecoder.decode("invalid-token"))
                .willThrow(new BadJwtException("invalid"));

        InvalidGoogleIdTokenException exception = assertThrows(
                InvalidGoogleIdTokenException.class,
                () -> googleTokenVerifier.verify("invalid-token")
        );

        assertThat(exception).hasMessage("유효하지 않은 Google ID Token입니다.");
    }
}
