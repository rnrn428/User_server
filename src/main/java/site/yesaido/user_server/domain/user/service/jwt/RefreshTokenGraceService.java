package site.yesaido.user_server.domain.user.service.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.entity.en.Role;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenGraceService {
    private static final Duration GRACE_TTL = Duration.ofSeconds(5);
    private static final String GRACE_KEY_PREFIX = "RT:grace:";

    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;

    public void saveReissueResponse(String oldRefreshToken, TokenResponse response) {
        String oldRefreshTokenHash = refreshTokenService.hashRefreshToken(oldRefreshToken);

        String value = response.getAccessToken() + "|" + response.getRefreshToken() + "|" + response.getRole() + "|" + response.getAccessTokenExpiresAt();

        redisTemplate.opsForValue().set(
                GRACE_KEY_PREFIX + oldRefreshTokenHash,
                value,
                GRACE_TTL
        );
    }

    public Optional<TokenResponse> findReissueResponse(
            String oldRefreshToken
    ) {
        String oldRefreshTokenHash = refreshTokenService.hashRefreshToken(oldRefreshToken);

        String value = redisTemplate.opsForValue().get(GRACE_KEY_PREFIX + oldRefreshTokenHash);

        if (value == null) {
            return Optional.empty();
        }

        String[] parts = value.split("\\|", 4);

        if (parts.length != 4) {
            return Optional.empty();
        }

        return Optional.of(
                TokenResponse.builder()
                    .accessToken(parts[0])
                    .refreshToken(parts[1])
                    .role(Role.valueOf(parts[2]))
                    .accessTokenExpiresAt(
                            Long.parseLong(parts[3])
                    )
                    .build()
        );
    }
}
