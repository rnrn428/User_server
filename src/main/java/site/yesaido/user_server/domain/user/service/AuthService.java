package site.yesaido.user_server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.login.PasswordResetRequest;
import site.yesaido.user_server.domain.user.dto.oauth.GoogleLoginRequest;
import site.yesaido.user_server.domain.user.dto.token.RefreshTokenRotation;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.jwt.AccessTokenBlacklistService;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenGraceService;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenService;
import site.yesaido.user_server.global.jwt.AccessTokenProvider;
import site.yesaido.user_server.global.oauth.GoogleTokenVerifier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenGraceService refreshTokenGraceService;
    private final AccessTokenBlacklistService accessTokenBlacklistService;
    private final GoogleTokenVerifier googleTokenVerifier;


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
                    User newUser = new User(verifiedEmail, uniqueNickname);
                    return userRepository.save(newUser);
                });

        return createTokenResponse(user);
    }


    @Transactional
    public TokenResponse reissue(String refreshToken){
        RefreshTokenRotation rotation;
        try{
            rotation = refreshTokenService.rotateRefreshToken(refreshToken);
        }catch (InvalidTokenException e){
            return refreshTokenGraceService.findReissueResponse(refreshToken)
                    .filter(response -> refreshTokenService.isRefreshTokenActive(
                            response.getRefreshToken()
                            )
                    ).orElseThrow(() -> e);
        }
        User user = userRepository.findById(rotation.userId()).orElseThrow(() -> new UserNotFoundException("유저를 찾을 수 없습니다."));

        TokenResponse response = buildTokenResponse(user, rotation.refreshToken());

        refreshTokenGraceService.saveReissueResponse(refreshToken, response);
        return response;
    }

    @Transactional
    public void logout(String refreshToken, String accessToken){
        refreshTokenService.revokeRefreshToken(refreshToken);
        if (accessToken != null && !accessToken.isBlank()){
            accessTokenBlacklistService.blacklist(accessToken);
        }
    }

    @Transactional
    public void releaseDormant(String email){
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        user.activate();
    }

    @Transactional
    public void resetPassword(PasswordResetRequest resetRequest){
        User user = userRepository.findByEmail(resetRequest.email().trim())
                .orElseThrow(() -> new UserNotFoundException("없는 사용자입니다."));

        if(UserStatus.DELETED.equals(user.getStatus())){
            throw new AlreadyWithdrawnException();
        }

        if(user.getPassword() == null){
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다.");
        }

        if(passwordEncoder.matches(resetRequest.newPassword(), user.getPassword())){
            throw new InvalidPasswordException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        user.updatePassword(passwordEncoder.encode(resetRequest.newPassword()));

        refreshTokenService.revokeAllRefreshTokens(user.getId());
    }


    private TokenResponse createTokenResponse(User user){
        validateLoginAllowed(user);
        String refreshToken = refreshTokenService.createRefreshTokenForUser(user.getId());

        return buildTokenResponse(user, refreshToken);
    }

    private TokenResponse buildTokenResponse(User user, String refreshToken){
        if(UserStatus.DELETED.equals(user.getStatus())){
            throw new AlreadyWithdrawnException("탈퇴한 사용자입니다.");
        }

        if(UserStatus.DORMANT.equals(user.getStatus())){
            throw new DormantUserException("휴면 계정입니다. 이메일 인증을 진행해 주세요.");
        }

        String accessToken = accessTokenProvider.createAccessToken(user.getId(), user.getRole());
        user.updateLastLoginAt();


        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .accessTokenExpiresAt(accessTokenProvider.getExpirationTime(accessToken))
                .build();
    }

    private void validateLoginAllowed(User user){
        if(UserStatus.DELETED.equals(user.getStatus())){
            throw new AlreadyWithdrawnException("탈퇴한 사용자입니다.");
        }
        if(UserStatus.DORMANT.equals(user.getStatus())){
            throw new DormantUserException("휴면 계정입니다. 이메일 인증을 진행해 주세요.");
        }
    }

}
