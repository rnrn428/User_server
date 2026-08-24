package site.yesaido.user_server.global.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.user_server.domain.user.entity.en.Role;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET_KEY = "dGVzdC1qd3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmctZm9yLWhzMjU2LWFsZ29yaXRobS10ZXN0";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpireTime", 1800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpireTime", 1209600000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("성공 : accessToken 발급 성공")
    void createAccessToken_success() {
        Long userId = 1L;
        Role role = Role.USER;

        String accessToken = jwtTokenProvider.createAccessToken(userId, role);

        assertThat(accessToken).isNotNull();
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
    }

    @Test
    @DisplayName("성공 : Access Token에서 실제 만료 시각을 추출한다")
    void getExpirationTime_success() {
        long beforeIssue = System.currentTimeMillis();
        String accessToken = jwtTokenProvider.createAccessToken(1L, Role.USER);

        long expirationTime = jwtTokenProvider.getExpirationTime(accessToken);

        assertThat(expirationTime).isBetween(
                // JWT exp 클레임은 초 단위로 저장되므로, 최대 999ms 정도 작아질 수 있다.
                beforeIssue + 1_800_000L - 1_000L,
                System.currentTimeMillis() + 1_800_000L
        );
    }

    @Test
    @DisplayName("성공 : Refresh Token이 정상 생성된다")
    void createRefreshToken_success() {
        Long userId = 1L;

        String refreshToken = jwtTokenProvider.createRefreshToken(userId, Role.USER);

        assertThat(refreshToken).isNotNull();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("성공 : 생성된 토큰에서 userId를 추출한다")
    void getUserIdFromToken_success() {
        Long userId = 4L;
        String accessToken = jwtTokenProvider.createAccessToken(userId, Role.USER);

        Long extractUserId = jwtTokenProvider.getUserIdFromToken(accessToken);

        assertThat(extractUserId).isEqualTo(4L);
    }

    @Test
    @DisplayName("성공 : 생성된 토큰에서 Role을 추출한다")
    void getRoleFromToken_success() {
        Long userId = 4L;
        String accessToken = jwtTokenProvider.createAccessToken(userId, Role.ADMIN);

        Role role = jwtTokenProvider.getRoleFromToken(accessToken);

        assertThat(role).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("실패 : 조작되거나 잘못된 토큰 검증 시 false를 반환한다")
    void validateToken_invalidToken() {
        String invalidToken = "yesaido-nhn-super-team";
        boolean token = jwtTokenProvider.validateToken(invalidToken);

        assertThat(token).isFalse();
    }

    @Test
    @DisplayName("실패 : 만료된 토큰 검증 시 false 반환 분기")
    void validateToken_expiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(expiredProvider, "accessTokenExpireTime", -1000L);
        ReflectionTestUtils.setField(expiredProvider, "refreshTokenExpireTime", -1000L);
        expiredProvider.init();

        String expiredToken = expiredProvider.createAccessToken(1L, Role.USER);
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("실패 : null 또는 빈문자열 토큰 검증 시 false 반환 분기")
    void validateToken_nullOrEmpty() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("성공 : role claim이 없는 토큰에서 Role 추출 시 기본값 Role.USER 반환 분기")
    void getRoleFromToken_nullRole_returnsDefaultUser() {
        java.util.Date now = new java.util.Date();
        java.util.Date validity = new java.util.Date(now.getTime() + 1800000L);
        java.security.Key key = (java.security.Key) ReflectionTestUtils.getField(jwtTokenProvider, "key");

        String tokenWithoutRole = io.jsonwebtoken.Jwts.builder()
                .setSubject("10")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        Role role = jwtTokenProvider.getRoleFromToken(tokenWithoutRole);

        assertThat(role).isEqualTo(Role.USER);
    }
}
