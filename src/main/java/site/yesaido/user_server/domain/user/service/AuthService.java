package site.yesaido.user_server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final GoogleTokenVerifier googleTokenVerifier;

    private static final Duration GRACE_PERIOD = Duration.ofSeconds(5);
    private static final String GRACE_KEY_PREFIX = "RT:grace:";

    @Transactional
    public TokenResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        log.info("[로그인 시도] 유저 DB상태: {}", user.getStatus());


        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new InvalidPasswordException();
        }

        return createTokenResponse(user);
    }

    @Transactional
    public TokenResponse loginWithGoogle(GoogleLoginRequest request){
        String verifiedEmail = googleTokenVerifier.verifyAndGetEmail(request.idToken());
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new InvalidTokenException("유효하지 않은 Google ID Token입니다.");
        }

        String baseNick = (request.nickName() != null && !request.nickName().isBlank())
                ? request.nickName().trim()
                : "google_user";

        if (baseNick.length() > 35) {
            baseNick = baseNick.substring(0, 35);
        }
        String uniqueNickname = baseNick + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);

        User user = userRepository.findByEmail(verifiedEmail)
                .orElseGet(() -> {
                    log.info("[구글 신규 소셜 회원가입] 이메일: {}, 닉네임: {}", verifiedEmail, uniqueNickname);
                    User newUser = new User(verifiedEmail, uniqueNickname);
                    return userRepository.save(newUser);
                });

        return createTokenResponse(user);
    }


    public TokenResponse reissue(String refreshToken){
        if(!jwtTokenProvider.validateToken(refreshToken)){
            throw new InvalidTokenException("유효하지 않거나 만료된 RefreshToken 입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        String savedRefreshToken = stringRedisTemplate.opsForValue().get("RT:" + userId);

        if(refreshToken.equals(savedRefreshToken)) {
            return rotateRefreshToken(userId, refreshToken);
        }

        TokenResponse graceResponse = readGraceResponse(userId, refreshToken);
        if (graceResponse != null) {
            return graceResponse;
        }
        throw new InvalidTokenException("레디스 토큰과 일치하지 않습니다. (로그아웃 또는 해킹 위험이 있습니다.)");
    }

    @Transactional
    public void logout(Long userId){
        stringRedisTemplate.delete("RT:" + userId);
    }

    @Transactional
    public void releaseDormant(String email){
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        user.activate();
    }

    private TokenResponse createTokenResponse(User user){
        if(UserStatus.DELETED.equals(user.getStatus())){
            throw new AlreadyWithdrawnException("탈퇴한 사용자입니다.");
        }

        if(UserStatus.DORMANT.equals(user.getStatus())){
            throw new DormantUserException("휴면 계정입니다. 이메일 인증을 진행해 주세요.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole());

        stringRedisTemplate.opsForValue().set(
                "RT:" + user.getId(),
                refreshToken,
                14,
                TimeUnit.DAYS
        );

        user.updateLastLoginAt();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .accessTokenExpiresAt(
                        jwtTokenProvider.getExpirationTime(accessToken)
                )
                .build();
    }

    private TokenResponse rotateRefreshToken(Long userId, String oldRefreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, user.getRole());

        stringRedisTemplate.opsForValue().set("RT:" + user.getId(), newRefreshToken, 14, TimeUnit.DAYS);

        TokenResponse response = TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .role(user.getRole())
                .accessTokenExpiresAt(
                        jwtTokenProvider.getExpirationTime(newAccessToken)
                )
                .build();

        saveGraceResponse(oldRefreshToken, response);
        return response;
    }

    private void saveGraceResponse(String oldRefreshToken, TokenResponse response) {
        String value = response.getAccessToken() + "|" + response.getRefreshToken() + "|" + response.getRole() + "|" + response.getAccessTokenExpiresAt();
        stringRedisTemplate.opsForValue().set(GRACE_KEY_PREFIX + oldRefreshToken, value, GRACE_PERIOD);
    }

    private TokenResponse readGraceResponse(Long userId, String oldRefreshToken){
        String value = stringRedisTemplate.opsForValue().get(GRACE_KEY_PREFIX + oldRefreshToken);
        if (value == null) {
            return null;
        }

        String[] parts = value.split("\\|", 4);
        if (parts.length != 4) {
            return null;
        }

        String graceRefreshToken = parts[1];
        String currentRefreshToken = stringRedisTemplate.opsForValue().get("RT:" + userId);
        if (currentRefreshToken == null || !currentRefreshToken.equals(graceRefreshToken)) {
            return null;
        }

        return TokenResponse.builder()
                .accessToken(parts[0])
                .refreshToken(graceRefreshToken)
                .role(Role.valueOf(parts[2]))
                .accessTokenExpiresAt(Long.parseLong(parts[3]))
                .build();
    }
}
