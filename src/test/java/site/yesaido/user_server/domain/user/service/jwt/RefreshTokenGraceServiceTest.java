package site.yesaido.user_server.domain.user.service.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.entity.en.Role;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenGraceServiceTest {
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("Grace 응답은 옛 RT의 해시 키로 5초 동안 저장한다")
    void saveReissueResponseStoresResponseForFiveSeconds() {
        RefreshTokenGraceService graceService = new RefreshTokenGraceService(refreshTokenService, redisTemplate);
        TokenResponse response = response();
        given(refreshTokenService.hashRefreshToken("old-refresh")).willReturn("old-hash");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        graceService.saveReissueResponse("old-refresh", response);

        verify(valueOperations).set(
                "RT:grace:old-hash",
                "access-token|refresh-token|USER|1000",
                Duration.ofSeconds(5)
        );
    }

    @Test
    @DisplayName("저장된 Grace 응답은 TokenResponse로 복원한다")
    void findReissueResponseRestoresTokenResponse() {
        RefreshTokenGraceService graceService = new RefreshTokenGraceService(refreshTokenService, redisTemplate);
        given(refreshTokenService.hashRefreshToken("old-refresh")).willReturn("old-hash");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:grace:old-hash"))
                .willReturn("access-token|refresh-token|USER|1000");

        TokenResponse response = graceService.findReissueResponse("old-refresh").orElseThrow();

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        assertThat(response.getAccessTokenExpiresAt()).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("Grace 기록이 없거나 형식이 잘못되면 빈 값을 반환한다")
    void findReissueResponseReturnsEmptyWhenMissingOrMalformed() {
        RefreshTokenGraceService graceService = new RefreshTokenGraceService(refreshTokenService, redisTemplate);
        given(refreshTokenService.hashRefreshToken("old-refresh")).willReturn("old-hash");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:grace:old-hash")).willReturn(null, "broken-value");

        assertThat(graceService.findReissueResponse("old-refresh")).isEmpty();
        assertThat(graceService.findReissueResponse("old-refresh")).isEmpty();
    }

    private TokenResponse response() {
        return TokenResponse.builder().accessToken("access-token").refreshToken("refresh-token")
                .role(Role.USER).accessTokenExpiresAt(1_000L).build();
    }
}
