package site.yesaido.user_server.domain.user.dto.token;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank(message = "RefreshToken은 필수입니다.")
        String refreshToken,
        String accessToken
) {
}
