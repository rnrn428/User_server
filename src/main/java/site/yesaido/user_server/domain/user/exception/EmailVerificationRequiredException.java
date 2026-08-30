package site.yesaido.user_server.domain.user.exception;

public class EmailVerificationRequiredException extends RuntimeException {
    public EmailVerificationRequiredException() {
        super("이메일 인증을 완료해 주세요.");
    }
}
