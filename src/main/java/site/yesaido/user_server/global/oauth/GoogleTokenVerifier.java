package site.yesaido.user_server.global.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {
    private final JwtDecoder googleJwtDecoder;

    public GoogleIdentity verify(String idToken){
        try{
            Jwt jwt = googleJwtDecoder.decode(idToken);

            Boolean emailVerified = jwt.getClaim("email_verified");
            if(!Boolean.TRUE.equals(emailVerified)){
                throw new IllegalArgumentException("Google 이메일 인증이 완료되지 않은 계정입니다.");
            }
            return new GoogleIdentity(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name")
            );
        }catch (JwtException e){
            log.warn("Google ID Token 검증 실패 : {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 Google ID Token입니다.");
        }
    }
}
