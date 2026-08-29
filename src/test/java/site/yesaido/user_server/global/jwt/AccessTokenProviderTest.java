package site.yesaido.user_server.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.user_server.domain.user.entity.en.Role;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenProviderTest {
    private static final String SECRET_KEY = "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmctZm9yLWhzMjU2LWFsZ29yaXRobS10ZXN0";
    private AccessTokenProvider accessTokenProvider;

    @BeforeEach
    void setUp() {
        accessTokenProvider = new AccessTokenProvider();
        ReflectionTestUtils.setField(accessTokenProvider, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(accessTokenProvider, "accessTokenExpireTime", 1_800_000L);
        accessTokenProvider.init();
    }

    @Test
    @DisplayName("발급한 Access Token은 ACCESS 타입이며 사용자 정보와 jti를 가진다")
    void createAccessTokenContainsRequiredClaims() {
        String accessToken = accessTokenProvider.createAccessToken(4L, Role.ADMIN);

        assertThat(accessTokenProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(accessTokenProvider.getUserId(accessToken)).isEqualTo(4L);
        assertThat(accessTokenProvider.getRoleFromToken(accessToken)).isEqualTo(Role.ADMIN);
        assertThat(accessTokenProvider.getTokenId(accessToken)).isNotBlank();
    }

    @Test
    @DisplayName("Access Token에서 실제 만료 시각을 추출한다")
    void getExpirationTimeReturnsTokenExpiration() {
        long beforeIssue = System.currentTimeMillis();
        String accessToken = accessTokenProvider.createAccessToken(1L, Role.USER);

        assertThat(accessTokenProvider.getExpirationTime(accessToken)).isBetween(
                beforeIssue + 1_800_000L - 1_000L,
                System.currentTimeMillis() + 1_800_000L
        );
    }

    @Test
    @DisplayName("ACCESS 타입이 없는 JWT는 Access Token으로 인정하지 않는다")
    void tokenWithoutAccessTypeIsRejected() {
        Key key = (Key) ReflectionTestUtils.getField(accessTokenProvider, "key");
        String tokenWithoutType = Jwts.builder().setSubject("1")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256).compact();

        assertThat(accessTokenProvider.validateAccessToken(tokenWithoutType)).isFalse();
    }

    @Test
    @DisplayName("만료되었거나 위조된 토큰은 검증에 실패한다")
    void invalidOrExpiredTokenIsRejected() {
        AccessTokenProvider expiredProvider = new AccessTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(expiredProvider, "accessTokenExpireTime", -1_000L);
        expiredProvider.init();

        assertThat(accessTokenProvider.validateAccessToken(
                expiredProvider.createAccessToken(1L, Role.USER))).isFalse();
        assertThat(accessTokenProvider.validateAccessToken("not-a-jwt")).isFalse();
        assertThat(accessTokenProvider.validateAccessToken(null)).isFalse();
    }
}
