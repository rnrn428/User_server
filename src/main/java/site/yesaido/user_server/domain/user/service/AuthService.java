package site.yesaido.user_server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.yesaido.user_server.domain.email.service.EmailService;
import site.yesaido.user_server.domain.user.dto.login.LoginRequest;
import site.yesaido.user_server.domain.user.dto.login.PasswordResetRequest;
import site.yesaido.user_server.domain.user.dto.oauth.GoogleLoginRequest;
import site.yesaido.user_server.domain.user.dto.token.RefreshTokenRotation;
import site.yesaido.user_server.domain.user.dto.token.TokenResponse;
import site.yesaido.user_server.domain.user.entity.OAuthUser;
import site.yesaido.user_server.domain.user.entity.User;
import site.yesaido.user_server.domain.user.entity.en.UserStatus;
import site.yesaido.user_server.domain.user.exception.*;
import site.yesaido.user_server.domain.user.repository.OAuthUserRepository;
import site.yesaido.user_server.domain.user.repository.UserRepository;
import site.yesaido.user_server.domain.user.service.jwt.AccessTokenBlacklistService;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenGraceService;
import site.yesaido.user_server.domain.user.service.jwt.RefreshTokenService;
import site.yesaido.user_server.global.jwt.AccessTokenProvider;
import site.yesaido.user_server.global.oauth.GoogleIdentity;
import site.yesaido.user_server.global.oauth.GoogleTokenVerifier;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private static final String GOOGLE_PROVIDER = "GOOGLE";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenGraceService refreshTokenGraceService;
    private final AccessTokenBlacklistService accessTokenBlacklistService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailService emailService;
    private final OAuthUserRepository oAuthUserRepository;


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
        GoogleIdentity identity = googleTokenVerifier.verify(request.idToken());

        User user = oAuthUserRepository.findByProviderAndProviderSubjectId(GOOGLE_PROVIDER, identity.subject()).map(OAuthUser::getUser)
                .orElseGet(()->createOrLinkGoogleUser(identity));

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
            throw new SocialLoginPasswordResetNotAllowedException("소셜 로그인 계정은 비밀번호를 재설정할 수 없습니다.");
        }

        if(passwordEncoder.matches(resetRequest.newPassword(), user.getPassword())){
            throw new InvalidPasswordException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        user.updatePassword(passwordEncoder.encode(resetRequest.newPassword()));

        refreshTokenService.revokeAllRefreshTokens(user.getId());
    }

    @Transactional
    public void sendPasswordResetEmail(String email){
        String normalizedEmail = email.trim();

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(()-> new UserNotFoundException("유저를 찾을 수 없습니다."));

        if(user.getStatus() == UserStatus.DELETED){
            throw new AlreadyWithdrawnException();
        }

        if(user.getPassword() == null){
            throw new SocialLoginPasswordResetNotAllowedException("Google로 가입한 계정입니다. Google로 로그인해 주세요.");
        }
        emailService.sendVerificationEmail(normalizedEmail);

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

    private User createOrLinkGoogleUser(GoogleIdentity identity) {
        User user = userRepository.findByEmail(identity.email())
                .orElseGet(() -> userRepository.save(
                        new User(identity.email(), createGoogleNickname(identity.name()))
                ));

        oAuthUserRepository.save(
                OAuthUser.builder()
                        .user(user)
                        .provider(GOOGLE_PROVIDER)
                        .providerSubjectId(identity.subject())
                        .build()
        );

        return user;
    }

    private String createGoogleNickname(String name){
        String baseNickname = (name == null || name.isBlank()) ? "google_user" : name.trim();

        if(baseNickname.length() > 35){
            baseNickname = baseNickname.substring(0, 35);
        }

        return baseNickname + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

}
