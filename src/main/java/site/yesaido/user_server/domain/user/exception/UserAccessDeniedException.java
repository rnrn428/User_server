package site.yesaido.user_server.domain.user.exception;

public class UserAccessDeniedException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "관리자만 접근할 수 있습니다.";
    public UserAccessDeniedException() {super(DEFAULT_MESSAGE);}
    public UserAccessDeniedException(String message) {
        super(message);
    }
}
