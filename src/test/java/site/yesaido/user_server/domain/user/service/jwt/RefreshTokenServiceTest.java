package site.yesaido.user_server.domain.user.service.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import site.yesaido.user_server.domain.user.dto.token.RefreshTokenRotation;
import site.yesaido.user_server.domain.user.exception.InvalidTokenException;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private SetOperations<String, String> setOperations;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(redisTemplate);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpireTime", 1_000L);
    }

    @Test
    @DisplayName("RT를 랜덤 문자열로 발급하고 해시값만 Redis에 저장한다")
    void createRefreshTokenForUserStoresHashedToken() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        String refreshToken = refreshTokenService.createRefreshTokenForUser(1L);

        String tokenHash = refreshTokenService.hashRefreshToken(refreshToken);
        verify(valueOperations).set("RT:token:" + tokenHash, "1", Duration.ofMillis(1_000L));
        verify(setOperations).add("RT:user:1:tokens", tokenHash);
        verify(redisTemplate).expire("RT:user:1:tokens", Duration.ofMillis(1_000L));
        assertThat(refreshToken).isNotBlank().isNotEqualTo(tokenHash);
    }

    @Test
    @DisplayName("재발급 시 이전 RT 세션을 지우고 새 RT 세션을 만든다")
    void rotateRefreshTokenReplacesOldSession() {
        String oldRefreshToken = "old-refresh-token";
        String oldTokenHash = refreshTokenService.hashRefreshToken(oldRefreshToken);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(valueOperations.get("RT:token:" + oldTokenHash)).willReturn("1");
        given(setOperations.isMember("RT:user:1:tokens", oldTokenHash)).willReturn(true);

        RefreshTokenRotation rotation = refreshTokenService.rotateRefreshToken(oldRefreshToken);

        String newTokenHash = refreshTokenService.hashRefreshToken(rotation.refreshToken());
        assertThat(rotation.userId()).isEqualTo(1L);
        assertThat(rotation.refreshToken()).isNotEqualTo(oldRefreshToken);
        verify(valueOperations).set("RT:token:" + newTokenHash, "1", Duration.ofMillis(1_000L));
        verify(redisTemplate).delete("RT:token:" + oldTokenHash);
        verify(setOperations).remove("RT:user:1:tokens", oldTokenHash);
    }

    @Test
    @DisplayName("존재하지 않는 RT는 재발급할 수 없다")
    void rotateRefreshTokenRejectsUnknownToken() {
        String refreshToken = "unknown-refresh-token";
        String tokenHash = refreshTokenService.hashRefreshToken(refreshToken);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("RT:token:" + tokenHash)).willReturn(null);

        assertThrows(InvalidTokenException.class,
                () -> refreshTokenService.rotateRefreshToken(refreshToken));
    }

    @Test
    @DisplayName("로그아웃은 현재 RT 세션 하나만 폐기한다")
    void revokeRefreshTokenRemovesOnlyCurrentSession() {
        String refreshToken = "refresh-token";
        String tokenHash = refreshTokenService.hashRefreshToken(refreshToken);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(valueOperations.get("RT:token:" + tokenHash)).willReturn("1");
        given(setOperations.isMember("RT:user:1:tokens", tokenHash)).willReturn(true);

        refreshTokenService.revokeRefreshToken(refreshToken);

        verify(redisTemplate).delete("RT:token:" + tokenHash);
        verify(setOperations).remove("RT:user:1:tokens", tokenHash);
    }

    @Test
    @DisplayName("비밀번호 변경은 해당 사용자의 모든 RT 세션을 폐기한다")
    void revokeAllRefreshTokensRemovesEverySession() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members("RT:user:1:tokens")).willReturn(Set.of("hash-a", "hash-b"));

        refreshTokenService.revokeAllRefreshTokens(1L);

        verify(redisTemplate).delete("RT:token:hash-a");
        verify(redisTemplate).delete("RT:token:hash-b");
        verify(redisTemplate).delete("RT:user:1:tokens");
    }

    @Test
    @DisplayName("RT가 Redis에 존재하고 사용자 세트에도 포함될 때만 활성 상태다")
    void isRefreshTokenActiveChecksBothRedisRecords() {
        String refreshToken = "refresh-token";
        String tokenHash = refreshTokenService.hashRefreshToken(refreshToken);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(valueOperations.get("RT:token:" + tokenHash)).willReturn("1");
        given(setOperations.isMember("RT:user:1:tokens", tokenHash)).willReturn(true);

        assertThat(refreshTokenService.isRefreshTokenActive(refreshToken)).isTrue();
    }
}
