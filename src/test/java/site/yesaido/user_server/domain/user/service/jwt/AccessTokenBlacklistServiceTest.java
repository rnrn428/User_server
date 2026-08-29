package site.yesaido.user_server.domain.user.service.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import site.yesaido.user_server.global.jwt.AccessTokenProvider;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistServiceTest {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private AccessTokenProvider accessTokenProvider;

    @Test
    @DisplayName("유효한 Access Token은 남은 수명만큼 jti 블랙리스트에 저장한다")
    void blacklistStoresTokenIdUntilExpiration() {
        AccessTokenBlacklistService service = new AccessTokenBlacklistService(redisTemplate, accessTokenProvider);
        given(accessTokenProvider.validateAccessToken("access-token")).willReturn(true);
        given(accessTokenProvider.getExpirationTime("access-token"))
                .willReturn(System.currentTimeMillis() + 30_000L);
        given(accessTokenProvider.getTokenId("access-token")).willReturn("token-id");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        service.blacklist("access-token");

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("AT:blacklist:token-id"),
                org.mockito.ArgumentMatchers.eq("logout"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isBetween(Duration.ofSeconds(29), Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("만료되었거나 유효하지 않은 Access Token은 블랙리스트에 저장하지 않는다")
    void blacklistSkipsInvalidOrExpiredToken() {
        AccessTokenBlacklistService service = new AccessTokenBlacklistService(redisTemplate, accessTokenProvider);
        given(accessTokenProvider.validateAccessToken("invalid")).willReturn(false);
        given(accessTokenProvider.validateAccessToken("expired")).willReturn(true);
        given(accessTokenProvider.getExpirationTime("expired")).willReturn(System.currentTimeMillis() - 1L);

        service.blacklist("invalid");
        service.blacklist("expired");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("jti가 블랙리스트 키에 있으면 true를 반환한다")
    void isBlacklistedChecksRedisKey() {
        AccessTokenBlacklistService service = new AccessTokenBlacklistService(redisTemplate, accessTokenProvider);
        given(redisTemplate.hasKey("AT:blacklist:token-id")).willReturn(true);

        assertThat(service.isBlacklisted("token-id")).isTrue();
    }
}
