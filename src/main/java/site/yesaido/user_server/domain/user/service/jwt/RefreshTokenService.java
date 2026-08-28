package site.yesaido.user_server.domain.user.service.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.yesaido.user_server.domain.user.dto.token.RefreshTokenRotation;
import site.yesaido.user_server.domain.user.exception.InvalidTokenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final String TOKEN_KEY_PREFIX = "RT:token:";
    private static final String USER_TOKENS_KEY_PREFIX = "RT:user:";
    private static final String USER_TOKENS_KEY_SUFFIX = ":tokens";

    private final StringRedisTemplate redisTemplate;

    @Value("${spring.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpireTime;

    private final SecureRandom secureRandom = new SecureRandom();

    public String createRefreshTokenForUser(Long userId){
        String refreshToken = createRefreshToken();
        saveRedis(userId, refreshToken);
        return refreshToken;
    }


    public RefreshTokenRotation rotateRefreshToken(String refreshToken){
        String oldTokenHash = hashRefreshToken(refreshToken);
        Long userId = findUserIdByRefreshTokenHash(oldTokenHash);

        String newRefreshToken = createRefreshToken();

        saveRedis(userId, newRefreshToken);
        deleteRedis(userId, oldTokenHash);

        return new RefreshTokenRotation(userId, newRefreshToken);
    }



    public void revokeRefreshToken(String refreshToken) {
        String tokenHash = hashRefreshToken(refreshToken);
        Long userId = findUserIdByRefreshTokenHash(tokenHash);

        deleteRedis(userId, tokenHash);
    }

    public void revokeAllRefreshTokens(Long userId){
        String userTokenKey = getUserTokenSetRedisKey(userId);
        Set<String> tokenHashes = redisTemplate.opsForSet().members(userTokenKey);

        if(tokenHashes != null){
            for(String tokenHash : tokenHashes){
                redisTemplate.delete(getTokenRedisKey(tokenHash));
            }
        }
        redisTemplate.delete(userTokenKey);
    }

    private Long findUserIdByRefreshTokenHash(String tokenHash){
        String userIdValue = redisTemplate.opsForValue().get(getTokenRedisKey(tokenHash));

        if(userIdValue == null){
            throw new InvalidTokenException("유효하지 않거나 만료된 RefreshToken입니다.");
        }

        Long userId = Long.parseLong(userIdValue);

        Boolean belongsToUser = redisTemplate.opsForSet().isMember(getUserTokenSetRedisKey(userId), tokenHash);

        if(!Boolean.TRUE.equals(belongsToUser)){
            throw new InvalidTokenException("Redis 토큰 정보가 일치하지 않습니다.");
        }
        return userId;
    }

    private void saveRedis(Long userId, String refreshToken){
        String tokenHash = hashRefreshToken(refreshToken);
        Duration ttl = Duration.ofMillis(refreshTokenExpireTime);

        redisTemplate.opsForValue().set(getTokenRedisKey(tokenHash), userId.toString(), ttl);

        redisTemplate.opsForSet().add(getUserTokenSetRedisKey(userId), tokenHash);

        redisTemplate.expire(getUserTokenSetRedisKey(userId), ttl);
    }

    private void deleteRedis(Long userId, String tokenHash){
        redisTemplate.delete(getTokenRedisKey(tokenHash));
        redisTemplate.opsForSet().remove(getUserTokenSetRedisKey(userId), tokenHash);

        Long remainingSessionCount = redisTemplate.opsForSet().size(getUserTokenSetRedisKey(userId));

        if(remainingSessionCount != null && remainingSessionCount == 0){
            redisTemplate.delete(getUserTokenSetRedisKey(userId));
        }
    }


    private String createRefreshToken(){
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashRefreshToken(String refreshToken){
        try{
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String getTokenRedisKey(String tokenHash){
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private String getUserTokenSetRedisKey(Long userId){
        return USER_TOKENS_KEY_PREFIX + userId + USER_TOKENS_KEY_SUFFIX;
    }

    public boolean isRefreshTokenActive(String refreshToken){
        try{
            String tokenHash = hashRefreshToken(refreshToken);
            findUserIdByRefreshTokenHash(tokenHash);
            return true;
        }catch (InvalidTokenException e){
            return false;
        }
    }

}
