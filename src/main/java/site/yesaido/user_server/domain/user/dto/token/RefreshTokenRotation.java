package site.yesaido.user_server.domain.user.dto.token;

public record RefreshTokenRotation(
        Long userId,
        String refreshToken
) {
}
