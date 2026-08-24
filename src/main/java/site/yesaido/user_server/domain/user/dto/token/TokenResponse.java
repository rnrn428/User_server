package site.yesaido.user_server.domain.user.dto.token;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.yesaido.user_server.domain.user.entity.en.Role;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Role role;
    private Long accessTokenExpiresAt;
}
