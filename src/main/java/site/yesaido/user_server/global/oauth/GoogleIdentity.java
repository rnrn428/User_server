package site.yesaido.user_server.global.oauth;

public record GoogleIdentity(
        String subject,
        String email,
        String name
) {
}
