package site.yesaido.user_server.global.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class GoogleJwtConfig {
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @Bean
    public JwtDecoder googleJwtDecoder(@Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId){
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(GOOGLE_ISSUER).build();

        OAuth2TokenValidator<Jwt> issuerAndExpirationValidator = JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER); // iss가 구글인지, exp가 지났는지 검사
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience().contains(clientId)){
                return OAuth2TokenValidatorResult.success(); // 우리 프로젝트 클라이언트 ID를 포함됐는지 검사
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "Google ID Token의 대상 앱이 일치하지 않습니다.", null);

            return OAuth2TokenValidatorResult.failure(error);
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndExpirationValidator, audienceValidator));

        return decoder;
    }

}
