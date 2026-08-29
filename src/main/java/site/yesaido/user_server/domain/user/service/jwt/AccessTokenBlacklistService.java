package site.yesaido.user_server.domain.user.service.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.yesaido.user_server.global.jwt.AccessTokenProvider;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistService {
    private static final String BLACKLIST_KEY_PREFIX = "AT:blacklist:";
    private final StringRedisTemplate redisTemplate;
    private final AccessTokenProvider accessTokenProvider;

    public void blacklist(String accessToken){
        if(!accessTokenProvider.validateAccessToken(accessToken)){
            return;
        }
        long expirationTime = accessTokenProvider.getExpirationTime(accessToken);
        long remainingMills = expirationTime - System.currentTimeMillis();

        if(remainingMills <= 0){
            return;
        }

        String tokenId = accessTokenProvider.getTokenId(accessToken);

        redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + tokenId, "logout", Duration.ofMillis(remainingMills));
    }

    public boolean isBlacklisted(String tokenId){
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + tokenId));
    }
}
